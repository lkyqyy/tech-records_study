/**
 * Script: sample_nonoverlap_nonwhite_rois.groovy
 *
 * Description:
 *   在整张图像中随机采样固定尺寸的矩形区域，满足：
 *     - 区域为非空白（RGB 平均值低于 whiteThreshold）
 *     - 与已有采样区域无重叠
 *   常用于采集负样本 patch（无组织、无标注目标的背景区域）。
 *
 * Parameters:
 *   roiSize        - 每个采样 patch 的边长（单位：像素）
 *   numSamples     - 目标采样区域数量
 *   maxTries       - 最大尝试次数（防止死循环）
 *   whiteThreshold - RGB 平均值高于此阈值视为空白区域（范围 0~255）
 *   downsample     - 图像缩放读取比例（用于加速空白判断）
 *
 * Output:
 *   在图像中添加多个 RectangleROI 注释，可用于后续导出 patch。
 *
 * Author: [Your Name]
 * Date: [Optional]
 */

import qupath.lib.roi.ROIs
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.regions.ImagePlane
import qupath.lib.regions.RegionRequest

import java.util.Random
import java.awt.image.BufferedImage
import java.awt.geom.Rectangle2D

// ---------- 参数设置 ----------
def roiSize = 640            // 每个 ROI 宽高（像素）
def numSamples = 150         // 目标采样数量
def maxTries = 5000          // 最大尝试次数
def whiteThreshold = 250     // 平均 RGB > 250 视为空白区域
def downsample = 8.0         // 缩放读取加速图像判断

// ---------- 图像与状态准备 ----------
def imageData = getCurrentImageData()
def server = imageData.getServer()
def imgW = server.getWidth()
def imgH = server.getHeight()
def rand = new Random()

clearAnnotations()

def addedROIs = []
int count = 0
int tries = 0

// ---------- 采样主循环 ----------
while (count < numSamples && tries < maxTries) {
    tries++
    def x = rand.nextInt(imgW - roiSize)
    def y = rand.nextInt(imgH - roiSize)

    // 创建区域请求（缩略图）
    def region = RegionRequest.createInstance(server.getPath(), downsample, x, y, roiSize, roiSize)
    BufferedImage img = server.readBufferedImage(region)

    // ---------- 判断是否为空白区域 ----------
    def raster = img.getRaster()
    long sum = 0
    int total = img.getWidth() * img.getHeight()

    for (int i = 0; i < img.getWidth(); i++) {
        for (int j = 0; j < img.getHeight(); j++) {
            def r = raster.getSample(i, j, 0)
            def g = raster.getSample(i, j, 1)
            def b = raster.getSample(i, j, 2)
            sum += (r + g + b) / 3
        }
    }

    def avg = sum / total
    if (avg > whiteThreshold) continue  // 是空白区域，跳过

    // ---------- 判断是否重叠 ----------
    def rectNew = new Rectangle2D.Double(x, y, roiSize, roiSize)
    def isOverlap = addedROIs.any { rectNew.intersects(it) }
    if (isOverlap) continue  // 有重叠，跳过

    // ---------- 添加 ROI ----------
    def roi = ROIs.createRectangleROI(x, y, roiSize, roiSize, ImagePlane.getDefaultPlane())
    def annotation = new PathAnnotationObject(roi)
    addObject(annotation)

    addedROIs.add(rectNew)
    count++
    println "✅ 添加 ROI ${count}（非空白 + 无重叠）"
}

println "🎯 最终采样 ${count} 个 ROI，尝试 ${tries} 次完成"
