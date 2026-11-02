"""Generate tvOS-style Settings and Weather icons as VectorDrawable XML + SVG previews."""
import math, sys, os

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)


def f(v):
    return ('%.2f' % v).rstrip('0').rstrip('.')


def circle(cx, cy, r, ccw=False):
    s = 0 if ccw else 1
    return 'M%s,%sm-%s,0a%s,%s 0,1 %d,%s,0a%s,%s 0,1 %d,-%s,0z' % (
        f(cx), f(cy), f(r), f(r), f(r), s, f(2 * r), f(r), f(r), s, f(2 * r))


def gear(cx, cy, r_root, r_tip, teeth, tip_frac=0.42, root_frac=0.42):
    """Toothed outline: trapezoid teeth with slightly rounded look via 4 points per tooth."""
    pts = []
    step = 2 * math.pi / teeth
    for i in range(teeth):
        a = i * step - math.pi / 2
        rf = root_frac * step / 2
        tf = tip_frac * step / 2
        for ang, r in ((a - step / 2 + rf * 0.2, r_root), (a - tf * 1.25, r_root), (a - tf, r_tip), (a + tf, r_tip), (a + tf * 1.25, r_root)):
            pts.append((cx + r * math.cos(ang), cy + r * math.sin(ang)))
    return 'M' + 'L'.join('%s,%s' % (f(x), f(y)) for x, y in pts) + 'z'


def spokes(cx, cy, r_in, r_out, n, width):
    d = ''
    for i in range(n):
        a = i * 2 * math.pi / n - math.pi / 2
        ux, uy = math.cos(a), math.sin(a)
        px, py = -uy * width / 2, ux * width / 2
        p = [(cx + ux * r_in + px, cy + uy * r_in + py), (cx + ux * r_out + px, cy + uy * r_out + py),
             (cx + ux * r_out - px, cy + uy * r_out - py), (cx + ux * r_in - px, cy + uy * r_in - py)]
        d += 'M' + 'L'.join('%s,%s' % (f(x), f(y)) for x, y in p) + 'z'
    return d


def settings_parts(cx, cy, s):
    """Gear geometry scaled by s (s=1 -> 132 px wide gear)."""
    ring = gear(cx, cy, 57 * s, 66 * s, 24, tip_frac=0.5) + circle(cx, cy, 47 * s, ccw=True)
    inner_ring = circle(cx, cy, 41 * s) + circle(cx, cy, 34 * s, ccw=True)
    hub = circle(cx, cy, 13 * s) + circle(cx, cy, 6 * s, ccw=True)
    sp = spokes(cx, cy, 11 * s, 36 * s, 6, 6.5 * s)
    return ring, inner_ring, hub, sp


# ---------- gradient helpers (VectorDrawable + SVG) ----------
class Doc:
    def __init__(self, w, h, vw, vh, comment):
        self.w, self.h, self.vw, self.vh = w, h, vw, vh
        self.comment = comment
        self.vd, self.svg, self.defs, self.n = [], [], [], 0

    def path(self, d, fill, even_odd=False):
        """fill: '#AARRGGBB' or ('lin', x1,y1,x2,y2, [(off,color)]) or ('rad', cx,cy,r, [(off,color)])."""
        rule = ' android:fillType="evenOdd"' if even_odd else ''
        srule = ' fill-rule="evenodd"' if even_odd else ''
        if isinstance(fill, str):
            self.vd.append('    <path android:fillColor="%s"%s\n        android:pathData="%s" />' % (fill, rule, d))
            self.svg.append('<path d="%s" fill="%s" fill-opacity="%s"%s/>' % (d, svgc(fill), f(alpha(fill)), srule))
            return
        self.n += 1
        gid = 'g%d' % self.n
        stops_vd = '\n'.join('                <item android:offset="%s" android:color="%s" />' % (f(o), c) for o, c in fill[-1])
        stops_svg = ''.join('<stop offset="%s" stop-color="%s" stop-opacity="%s"/>' % (f(o), svgc(c), f(alpha(c))) for o, c in fill[-1])
        if fill[0] == 'lin':
            _, x1, y1, x2, y2, _s = fill
            g = ('android:type="linear" android:startX="%s" android:startY="%s" android:endX="%s" android:endY="%s"'
                 % (f(x1), f(y1), f(x2), f(y2)))
            self.defs.append('<linearGradient id="%s" gradientUnits="userSpaceOnUse" x1="%s" y1="%s" x2="%s" y2="%s">%s</linearGradient>'
                             % (gid, f(x1), f(y1), f(x2), f(y2), stops_svg))
        else:
            _, cx, cy, r, _s = fill
            g = 'android:type="radial" android:centerX="%s" android:centerY="%s" android:gradientRadius="%s"' % (f(cx), f(cy), f(r))
            self.defs.append('<radialGradient id="%s" gradientUnits="userSpaceOnUse" cx="%s" cy="%s" r="%s">%s</radialGradient>'
                             % (gid, f(cx), f(cy), f(r), stops_svg))
        self.vd.append('''    <path%s android:pathData="%s">
        <aapt:attr name="android:fillColor">
            <gradient %s>
%s
            </gradient>
        </aapt:attr>
    </path>''' % (rule, d, g, stops_vd))
        self.svg.append('<path d="%s" fill="url(#%s)"%s/>' % (d, gid, srule))

    def write(self, name):
        with open(os.path.join(OUT, name + '.xml'), 'w') as fh:
            fh.write('<?xml version="1.0" encoding="utf-8"?>\n<!-- %s -->\n' % self.comment)
            fh.write('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
                     '    xmlns:aapt="http://schemas.android.com/aapt"\n'
                     '    android:width="%sdp" android:height="%sdp"\n'
                     '    android:viewportWidth="%s" android:viewportHeight="%s">\n' % (self.w, self.h, self.vw, self.vh))
            fh.write('\n'.join(self.vd) + '\n</vector>\n')
        with open(os.path.join(OUT, name + '.svg'), 'w') as fh:
            fh.write('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %s %s" width="%s" height="%s"><defs>%s</defs>%s</svg>'
                     % (self.vw, self.vh, self.vw * 4, self.vh * 4, ''.join(self.defs), ''.join(self.svg)))


def svgc(c):
    return '#' + c[3:] if len(c) == 9 else c


def alpha(c):
    return int(c[1:3], 16) / 255 if len(c) == 9 else 1


def rrect(w, h, r):
    return 'M%s,0h%sa%s,%s 0,0 1,%s,%sv%sa%s,%s 0,0 1,-%s,%sh-%sa%s,%s 0,0 1,-%s,-%sv-%sa%s,%s 0,0 1,%s,-%sz' % (
        r, w - 2 * r, r, r, r, r, h - 2 * r, r, r, r, r, w - 2 * r, r, r, r, r, h - 2 * r, r, r, r, r)


METAL = [(0, '#FF9FA3AA'), (0.5, '#FF7B8089'), (1, '#FF5A5E66')]


def settings(doc, W, H, cx, cy, s, bg_d):
    doc.path(bg_d, ('lin', 0, 0, 0, H, [(0, '#FFF4F5F7'), (1, '#FFC4C7CD')]))
    ring, inner_ring, hub, sp = settings_parts(cx, cy, s)
    top, bot = cy - 66 * s, cy + 66 * s
    # soft drop shadow under the gear
    doc.path(circle(cx, cy + 4 * s, 68 * s), ('rad', cx, cy + 4 * s, 70 * s, [(0.88, '#22000000'), (1, '#00000000')]))
    doc.path(ring, ('lin', 0, top, 0, bot, METAL), even_odd=True)
    # bevel highlight on the ring's inner edge
    doc.path(circle(cx, cy, 47 * s) + circle(cx, cy, 45 * s, ccw=True), ('lin', 0, top, 0, bot, [(0, '#FF5E636B'), (1, '#FFD6D9DE')]), even_odd=True)
    doc.path(inner_ring, ('lin', 0, cy - 41 * s, 0, cy + 41 * s, METAL), even_odd=True)
    doc.path(sp, ('lin', 0, cy - 36 * s, 0, cy + 36 * s, METAL))
    doc.path(hub, ('lin', 0, cy - 13 * s, 0, cy + 13 * s, [(0, '#FFB4B8BE'), (1, '#FF61656D')]), even_odd=True)


CLOUD = 'M0,0h96a22,22 0,0 0,0 -44a30,30 0,0 0,-56 -10a24,24 0,0 0,-40 18a18,18 0,0 0,0 36z'


def cloud_path(x, y, s):
    # the add-on's own cloud shape, rebuilt at (x,y) scale s
    return ('M%s,%sh%sa%s,%s 0,0 0,0 -%sa%s,%s 0,0 0,-%s -%sa%s,%s 0,0 0,-%s %sa%s,%s 0,0 0,0 %sz' % (
        f(x), f(y), f(96 * s), f(22 * s), f(22 * s), f(44 * s), f(30 * s), f(30 * s), f(56 * s), f(10 * s),
        f(24 * s), f(24 * s), f(40 * s), f(18 * s), f(18 * s), f(18 * s), f(36 * s)))


def weather(doc, W, H, sun, sr, cloud_xy, cs, bg_d):
    doc.path(bg_d, ('lin', 0, 0, 0, H, [(0, '#FF1F6FD1'), (0.6, '#FF4A9BE6'), (1, '#FF7CC3F4')]))
    sx, sy = sun
    doc.path(circle(sx, sy, sr * 1.9), ('rad', sx, sy, sr * 1.9, [(0.45, '#66FFE680'), (1, '#00FFE680')]))
    doc.path(circle(sx, sy, sr), ('rad', sx - sr * 0.3, sy - sr * 0.3, sr * 1.4, [(0, '#FFFFF6B0'), (0.6, '#FFFFD23F'), (1, '#FFFFB300')]))
    x, y = cloud_xy
    doc.path(cloud_path(x + 2 * cs, y + 5 * cs, cs), '#1A0A3A70')
    doc.path(cloud_path(x, y, cs), ('lin', 0, y - 62 * cs, 0, y, [(0, '#FFFFFFFF'), (1, '#FFDCEAF8')]))


# Launcher banner for the Settings tile
d = Doc(320, 180, 320, 180, 'Settings tile banner (config.json app_banners): a metal gear on light grey, tvOS style. Drawn for this launcher.')
settings(d, 320, 180, 160, 90, 1.0, 'M0,0h320v180h-320z')
d.write('banner_ajustes')

# Weather add-on banner + square icon
d = Doc(320, 180, 320, 180, 'The TV banner: a sun behind a cloud on a blue day sky, tvOS style. Drawn for this add-on, no third-party art.')
weather(d, 320, 180, (196, 66), 32, (106, 132), 1.0, 'M0,0h320v180h-320z')
d.write('banner')
d = Doc(48, 48, 108, 108, "The app icon: the banner's sun and cloud, square.")
weather(d, 108, 108, (66, 40), 20, (24, 82), 0.6, 'M0,0h108v108h-108z')
d.write('icon')

# Monochrome gear glyph (Control Center round button, top bar, menus)
d = Doc(22, 22, 24, 24, 'Settings glyph: toothed ring, inner ring and six spokes, tvOS style.')
d.path(gear(12, 12, 8.4, 11, 12, tip_frac=0.55) + circle(12, 12, 6.2, ccw=True), '#FFFFFFFF', even_odd=True)
d.path(spokes(12, 12, 2, 6.4, 6, 1.5), '#FFFFFFFF')
d.path(circle(12, 12, 2.6) + circle(12, 12, 1.2, ccw=True), '#FFFFFFFF', even_odd=True)
d.write('ic_settings')
