# Python 工具脚本

本目录包含多个用于医学图像与大图场景下的实用 Python 工具脚本，适用于图像压缩、格式转换、训练集构建前的预处理等任务。

---

## 工具脚本
以下是各个脚本的功能和使用方法：

<details>
<summary><strong>1. compress_images_to_jpeg.py（图像压缩）</strong></summary>

**功能**：  
批量压缩 PNG/JPG 图像为 JPEG 格式，自动控制每张图像大小不超过指定阈值（默认 24MB）。适用于上传或模型推理前缩小图像。

**特点**：

- 支持超大图（关闭 PIL 像素限制）；
- 自动降低质量 + 缩小图像；
- 输出结果附带 `_compressed.jpg` 后缀；
- 跳过重复压缩。

**使用方式**：

在脚本中设置如下路径并运行：

```python
input_folder = "/home/lk/Project/IHC_TensorRT/data"
batch_compress(input_folder, max_size_mb=24)
