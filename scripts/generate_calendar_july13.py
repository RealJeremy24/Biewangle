"""
生成 ic_calendar_july13.png 日历图标
依赖：pip install pillow
运行：py -3 scripts/generate_calendar_july13.py
"""
from PIL import Image, ImageDraw, ImageFont
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
OUTPUT_PATH = os.path.join(PROJECT_ROOT, "app", "src", "main", "res", "drawable", "ic_calendar_july13.png")

W, H = 256, 256
BORDER = 6

# Transparent background
img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Layout constants
padding = 20
body_top = 30
body_bottom = H - 20
corner_radius = 28
header_height = 52

# === Black outer border (drawn first, slightly larger) ===
bw = BORDER  # border width

# Outer black border for white body
draw.rounded_rectangle(
    [padding - bw, body_top - bw, W - padding + bw, body_bottom + bw],
    radius=corner_radius + bw,
    fill=(60, 60, 60, 255)
)

# White body
draw.rounded_rectangle(
    [padding, body_top, W - padding, body_bottom],
    radius=corner_radius,
    fill=(255, 255, 255, 255)
)

# Outer black border for red header
draw.rounded_rectangle(
    [padding - bw, body_top - bw, W - padding + bw, body_top + header_height + bw],
    radius=corner_radius + bw,
    fill=(60, 60, 60, 255)
)

# Red header fill
draw.rounded_rectangle(
    [padding, body_top, W - padding, body_top + header_height],
    radius=corner_radius,
    fill=(220, 80, 60, 255)
)
# Cover bottom corners of header to make it flat at bottom
draw.rectangle(
    [padding + corner_radius, body_top + header_height - bw,
     W - padding - corner_radius, body_top + header_height],
    fill=(220, 80, 60, 255)
)

# "July" text in header
try:
    font_header = ImageFont.truetype("arialbd.ttf", 34)
except:
    font_header = ImageFont.load_default()

july_text = "July"
bbox = draw.textbbox((0, 0), july_text, font=font_header)
text_w = bbox[2] - bbox[0]
text_h = bbox[3] - bbox[1]
text_x = (W - text_w) // 2
text_y = body_top + (header_height - text_h) // 2 - 2
draw.text((text_x, text_y), july_text, fill=(255, 255, 255, 255), font=font_header)

# Large "13" centered in white area
try:
    font_large = ImageFont.truetype("arialbd.ttf", 88)
except:
    font_large = ImageFont.load_default()

date_text = "13"
bbox2 = draw.textbbox((0, 0), date_text, font=font_large)
date_w = bbox2[2] - bbox2[0]
date_h = bbox2[3] - bbox2[1]
date_x = (W - date_w) // 2
date_y = body_top + header_height + 10

# Shadow for depth
draw.text((date_x + 3, date_y + 3), date_text, fill=(200, 190, 170, 255), font=font_large)
draw.text((date_x, date_y), date_text, fill=(60, 60, 60, 255), font=font_large)

img.save(OUTPUT_PATH, "PNG")
print(f"Saved to: {OUTPUT_PATH}")
