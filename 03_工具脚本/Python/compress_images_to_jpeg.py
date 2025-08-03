import os
from PIL import Image

Image.MAX_IMAGE_PIXELS = None

"""
compress_images_to_jpeg.py

功能：
    - 批量压缩 PNG/JPG/JPEG 图像为 JPEG 格式；
    - 自动递归降低质量、缩放尺寸，使文件大小控制在 max_size_mb（默认 24MB）以内；
    - 自动跳过已压缩图像（文件名包含 "_compressed"）；
    - 输出压缩成功、跳过与失败的统计信息；
    - 输出图像保存至原目录，文件名追加 `_compressed.jpg`。

使用方式：
    - 修改 input_folder 路径；
    - 在终端执行：python compress_images_to_jpeg.py
"""

def compress_image(input_path, output_path, max_size_mb=95, quality=85, resize_factor=1.0, depth=0):
    try:
        img = Image.open(input_path)

        if resize_factor < 1.0:
            new_size = (int(img.width * resize_factor), int(img.height * resize_factor))
            img = img.resize(new_size, Image.LANCZOS)

        if img.mode != "RGB":
            img = img.convert("RGB")

        img.save(output_path, format="JPEG", quality=quality)
        size_mb = os.path.getsize(output_path) / (1024 * 1024)

        print(f"{'  '*depth}Saved: {os.path.basename(output_path)} | Quality={quality} | Scale={resize_factor:.3f} | Size={size_mb:.2f} MB")

        if size_mb > max_size_mb and quality > 30:
            return compress_image(
                input_path,
                output_path,
                max_size_mb=max_size_mb,
                quality=quality - 10,
                resize_factor=resize_factor * 0.95,
                depth=depth + 1
            )

        return True

    except Exception as e:
        print(f"{'  '*depth}Error compressing: {input_path} | {e}")
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
            skip_count += 1
            continue

        result = compress_image(input_path, output_path, max_size_mb=max_size_mb)
        if result:
            success_count += 1
        else:
            fail_count += 1

    print("\nSummary:")
    print(f"  Success: {success_count}")
    print(f"  Skipped: {skip_count}")
    print(f"  Failed:  {fail_count}")


if __name__ == "__main__":
    input_folder = "/home/lk/Project/IHC_TensorRT/data"  
    batch_compress(input_folder, max_size_mb=24)
