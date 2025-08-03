/**
 * Script: stardist_detect_and_classify.groovy
 *
 * Description:
 *   使用 StarDist 模型检测细胞，并根据 DAB 通道 OD 均值进行分类（ER+ / ER-），
 *   将结果转为可编辑注释对象，便于后续修正、导出。
 *
 * Requirements:
 *   - StarDist 2D 模型（TensorFlow .pb 格式）
 *   - 当前图像已选择目标区域（Rectangle / ROI）
 */

import qupath.ext.stardist.StarDist2D
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.objects.PathDetectionObject
import qupath.lib.objects.classes.PathClass

// ===== Step 1: 设置 StarDist 模型路径 =====
def modelPath = "E:/QuPath-v0.5.1-Windows/model/he_heavy_augment.pb"

// ===== Step 2: 构建 StarDist 检测器 =====
def stardist = StarDist2D.builder(modelPath)
    .threshold(0.1)                      // 分割概率阈值
    .normalizePercentiles(0.4, 99.8)     // 灰度归一化百分位
    .pixelSize(0.5)                      // 模型目标分辨率（μm/pixel）
    .tileSize(1024)                      // 滑窗尺寸
    .measureIntensity()
    .measureShape()
    .includeProbability(true)
    .nThreads(4)
    .build()

// ===== Step 3: 获取当前图像与所选 ROI 区域 =====
def imageData = getCurrentImageData()
def pathObjects = getSelectedObjects()

if (pathObjects.isEmpty()) {
    println("⚠️ 请先选择一个 ROI 区域再运行本脚本！")
    return
}

// ===== Step 4: 执行 StarDist 检测 =====
stardist.detectObjects(imageData, pathObjects)

def detections = getDetectionObjects()
println("✅ 检测完成，共检测到 ${detections.size()} 个细胞")

// ===== Step 5: 分类并转为注释对象（Annotation）=====
def channelName = "Cell: DAB OD mean"  // 用于分类的通道名
def threshold = 0.1                    // 分类阈值
def classPositive = PathClass.fromString("ER+")
def classNegative = PathClass.fromString("ER-")

// 可选：移除原始检测对象，避免冗余
removeObjects(detections, false)

def annotations = []

detections.each { d ->
    def roi = d.getROI()
    def value = d.getMeasurementList().get(channelName)
    def annotation = new PathAnnotationObject(roi)

    // 设置分类标签
    if (value != null) {
        annotation.setPathClass(value > threshold ? classPositive : classNegative)
    }

    // 可选：保留原始测量值
    annotation.getMeasurementList().putAll(d.getMeasurementList())

    annotations << annotation
}

// 添加分类注释对象
addObjects(annotations)
println("🎯 检测结果已转为注释对象（共 ${annotations.size()} 个），支持手动编辑与导出")
