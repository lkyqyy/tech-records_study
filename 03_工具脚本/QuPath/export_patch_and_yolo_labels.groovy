/**
 * Script: export_patch_and_yolo_labels.groovy
 *
 * Description:
 *   导出所有矩形标注区域为图像 patch（.png），并在每个 patch 中查找包含的非矩形标注（如细胞点状标注），
 *   若其中心点落在该矩形 patch 内，则输出 YOLO 格式标签 (.txt)。
 *
 * YOLO 标签格式：
 *   class_id x_center y_center box_width box_height   （坐标为相对 patch 尺寸的归一化值）
 *
 * 输入要求：
 *   - 矩形注释视为 patch；
 *   - 非矩形注释视为点状目标（细胞等）；
 *   - 类别由 `classMap` 映射；
 *
 * 输出结构：
 *   D:/out/
 *     ├── region_123.png
 *     └── region_123.txt
 */

import qupath.lib.regions.RegionRequest
import qupath.lib.images.writers.ImageWriterTools
import qupath.lib.roi.RectangleROI
import qupath.lib.roi.PolygonROI
import qupath.lib.roi.EllipseROI

def baseOutputDir = "D:/out"
mkdirs(baseOutputDir)

// 获取图像信息
def imageData = getCurrentImageData()
def server = imageData.getServer()
def imagePath = server.getPath()

// 获取所有注释
def rectangleAnnotations = getAnnotationObjects().findAll { it.getROI() instanceof RectangleROI }
def nonRectangleAnnotations = getAnnotationObjects().findAll { !(it.getROI() instanceof RectangleROI) }

print "INFO: 共找到 ${rectangleAnnotations.size()} 个矩形框标注区域\n"
print "INFO: 其中 ${nonRectangleAnnotations.size()} 个非矩形框标注区域\n"

// 类别映射表（细胞点标注的类别）
def classMap = [
    "ER+": 0,
    "ER-": 1
]

// 遍历每个矩形 patch 区域
for (def annotation : rectangleAnnotations) {
    def roi = annotation.getROI()
    if (roi == null) continue

    // patch 边界参数
    def minX = roi.getBoundsX()
    def minY = roi.getBoundsY()
    def patchWidth  = roi.getBoundsWidth()
    def patchHeight = roi.getBoundsHeight()

    // 类别目录与命名
    def rawClassName = annotation.getPathClass()?.toString() ?: "Region"
    def cleanedClass = rawClassName.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_").replaceAll("^_+|_+\$", "")
    def regionName = annotation.getName() ?: cleanedClass + "_" + annotation.hashCode()

    def classDir = buildFilePath(baseOutputDir, cleanedClass)
    mkdirs(classDir)

    def imageOutputPath = buildFilePath(classDir, regionName + ".png")
    def labelOutputPath = buildFilePath(classDir, regionName + ".txt")

    // 导出 patch 图像
    def request = RegionRequest.createInstance(imagePath, 1.0, roi)
    ImageWriterTools.writeImageRegion(server, request, imageOutputPath)
    print "✅ 导出 patch: ${imageOutputPath}\n"

    // ===== 搜索 patch 内的点状标注（非矩形）并生成标签 =====
    def pointsInPatch = []

    for (def nonRectAnn : nonRectangleAnnotations) {
        def cellROI = nonRectAnn.getROI()
        if (cellROI == null) continue

        // 获取标注中心点坐标
        def centerX = cellROI.getBoundsX() + cellROI.getBoundsWidth() / 2.0
        def centerY = cellROI.getBoundsY() + cellROI.getBoundsHeight() / 2.0

        // 判断是否落在当前 patch 内
        if (centerX >= minX && centerX <= minX + patchWidth &&
            centerY >= minY && centerY <= minY + patchHeight) {

            def label = classMap.get(nonRectAnn.getPathClass()?.toString())
            if (label != null) {
                def cx = (centerX - minX) / patchWidth
                def cy = (centerY - minY) / patchHeight
                pointsInPatch << [label, cx, cy]
            }
        }
    }

    // 写入 YOLO 格式标签文件
    def boxW = 0.05
    def boxH = 0.05
    def lines = pointsInPatch.collect {
        "${it[0]} ${String.format('%.6f %.6f %.6f %.6f', it[1], it[2], boxW, boxH)}"
    }

    new File(labelOutputPath).withWriter { w ->
        w.write(lines.join("\n"))
    }
    print "✅ 导出标签: ${labelOutputPath}（共 ${pointsInPatch.size()} 个）\n"
}
