import os
from PIL import Image

# 允许处理超大图像（如病理图）
Image.MAX_IMAGE_PIXELS = None

"""
图像批量压缩工具 - compress_images_to_jpeg.py
----------------------------------------------

功能：
    - 批量压缩 PNG/JPG/JPEG 图像为 JPEG 格式；
    - 自动递归降低质量、缩放图像，使文件大小控制在 max_size_mb（默认 24MB）以内；
    - 自动跳过文件名中已包含 "_compressed" 的图像；
    - 针对大图关闭 PIL 的像素限制；
    - 输出统计信息（成功、失败、跳过数量）；
    - 输出文件保存在同目录，文件名附加 `_compressed.jpg`。

使用：
    - 修改下方 input_folder 路径；
    - 双击或运行 `python compress_images_to_jpeg.py`；
    - 适用于图像上传前压缩、病理大图缩小保存等场景。
"""

def compress_image(input_path, output_path, max_size_mb=95, quality=85, resize_factor=1.0, depth=0):
    indent = "  " * depth
    try:
        img = Image.open(input_path)

        if resize_factor < 1.0:
            new_size = (int(img.width * resize_factor), int(img.height * resize_factor))
            img = img.resize(new_size, Image.LANCZOS)

        if img.mode != "RGB":
            img = img.convert("RGB")

        img.save(output_path, format="JPEG", quality=quality)
        size_mb = os.path.getsize(output_path) / (1024 * 1024)

        print(f"{indent}📦 尝试保存：{os.path.basename(output_path)} | 质量={quality} | 缩放={resize_factor:.3f} | 大小={size_mb:.2f} MB")

        if size_mb > max_size_mb and quality > 30:
            return compress_image(
                input_path,
                output_path,
                max_size_mb=max_size_mb,
                quality=quality - 10,
                resize_factor=resize_factor * 0.95,
                depth=depth + 1
            )

        print(f"{indent}✅ 压缩完成：{output_path}（{size_mb:.2f} MB）")
        return True

    except Exception as e:
        print(f"{indent}❌ 压缩失败：{input_path}，错误信息：{e}")
        return False


def batch_compress(folder, max_size_mb=24):
    success_count = 0
    skip_count = 0
    fail_count = 0

    for filename in os.listdir(folder):
        if not filename.lower().endswith((".png", ".jpg", ".jpeg")):
            continue
        if "_compressed" in filename:
            skip_count += 1
            continue

        input_path = os.path.join(folder, filename)
        name_no_ext = os.path.splitext(filename)[0]
        output_path = os.path.join(folder, f"{name_no_ext}_compressed.jpg")

        if os.path.exists(output_path):
            print(f"⚠️ 已存在，跳过：{output_path}")
            skip_count += 1
            continue

        result = compress_image(input_path, output_path, max_size_mb=max_size_mb)
        if result:
            success_count += 1
        else:
            fail_count += 1

    print("\n📊 批量压缩完成")
    print(f"  ✅ 成功：{success_count}")
    print(f"  ⚠️ 跳过：{skip_count}")
    print(f"  ❌ 失败：{fail_count}")


# 示例调用
if __name__ == "__main__":
    input_folder = "/home/lk/Project/IHC_TensorRT/data"  # 修改为你的图像目录路径
    batch_compress(input_folder, max_size_mb=24)
