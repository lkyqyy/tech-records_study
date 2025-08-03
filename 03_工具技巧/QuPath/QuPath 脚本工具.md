# QuPath 脚本工具

本目录收录了多个 QuPath 项目中常用的图像处理与推理辅助脚本，支持从命令行运行或在 QuPath 脚本编辑器中使用。适用于病理图像处理、patch 提取、模型推理与标注转换等任务。

## 工具列表

1. **export_rectangle_patches.groovy**  
   批量导出所有矩形 Annotation 区域为 patch 图像（适用于已有 ROI 区域的图像裁剪导出）。

2. **export_patch_and_yolo_labels.groovy**  
   将 patch 图像导出为 PNG，同时将其中的点注释导出为 YOLO 格式标签（.txt），用于目标检测训练。

3. **stardist_detect_and_classify.groovy**  
   调用 StarDist 模型进行细胞检测，按强度或其他属性分类后生成可编辑 Annotation，适合人工调整或导出使用。

4. **sample_nonoverlap_nonwhite_rois.groovy**  
   在全图范围内随机采样多个非重叠、非白底区域的矩形 ROI，适用于负样本/背景区域提取。

---

## 使用方法

- 所有脚本均在 **QuPath 的 Script Editor** 中直接运行，无需命令行；
- 脚本顶部一般会包含可修改的参数区域（如输出路径、patch 尺寸、模型路径等），运行前请根据实际项目进行配置；
- 建议在运行脚本前：
  - 打开目标图像；
  - 确保已加载项目（`.qpproj`）；
  - 已完成必要的标注（如矩形注释、点标注）；
- 部分脚本会生成文件（如 PNG 图像、YOLO 标签），请确保输出目录存在或由脚本创建。

标准使用流程如下：

1. 通过以下两种方式之一获取图像中的 ROI 区域（矩形 Annotation）：
   - **手动绘制 ROI**：直接在 QuPath 中使用矩形工具标注感兴趣区域；
   - 或运行脚本 **`sample_nonoverlap_nonwhite_rois.groovy`**，自动在图像中采样多个非重叠、非空白区域作为 ROI，用于生成背景或负样本 patch。

2. 运行脚本 **`stardist_detect_and_classify.groovy`**，对 ROI 区域内的图像执行 StarDist 模型推理，生成细胞检测结果，并以 Annotation 形式保存。

3. 运行脚本 **`export_patch_and_yolo_labels.groovy`**，导出每个 ROI 对应的图像 patch（PNG）及其中细胞的 YOLO 格式标签（TXT）。

> 说明：脚本 **`export_rectangle_patches.groovy`** 用于单独导出图像区域（不包含标签），常用于推理测试阶段的图像准备。
