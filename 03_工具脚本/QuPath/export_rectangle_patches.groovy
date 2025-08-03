import qupath.lib.regions.RegionRequest
import qupath.lib.images.writers.ImageWriterTools
import qupath.lib.roi.RectangleROI


def baseOutputDir = "D:/out"
mkdirs(baseOutputDir)

def imageData = getCurrentImageData()
def server = imageData.getServer()
def imagePath = server.getPath()

// 获取所有矩形框注释区域
def rectangleAnnotations = getAnnotationObjects().findAll {
    it.getROI() instanceof RectangleROI
}

// 遍历每个矩形区域并导出 patch 图像
for (def annotation : rectangleAnnotations) {
    def roi = annotation.getROI()
    if (roi == null) continue

    // 获取类别名称（默认使用 PathClass 或 "Region"）
    def className = annotation.getPathClass()?.toString() ?: "Region"
    className = className.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_").replaceAll("^_+|_+\$", "")

    // 生成保存路径
    def regionName = annotation.getName() ?: className + "_" + annotation.hashCode()
    def classDir = buildFilePath(baseOutputDir, className)
    mkdirs(classDir)
    def imageOutputPath = buildFilePath(classDir, regionName + ".png")

    // 导出图像
    def request = RegionRequest.createInstance(imagePath, 1.0, roi)
    ImageWriterTools.writeImageRegion(server, request, imageOutputPath)
}
