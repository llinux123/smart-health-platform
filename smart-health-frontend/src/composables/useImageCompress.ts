/**
 * 图片压缩公共 Composable
 *
 * 从 AccountInfoPage.vue 中提取的图片压缩逻辑，供实名认证页面复用。
 * 策略：Canvas 重绘 → JPEG 输出 → 两轮降级压缩确保 base64 不超限。
 */

/** Base64 字符串长度上限，与后端 body limit 对齐 */
const MAX_BASE64_LENGTH = 400_000

export function useImageCompress() {
  /** 将 File 转换为 base64 data URL */
  function fileToDataUrl(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(reader.result as string)
      reader.onerror = () => reject(new Error('文件读取失败'))
      reader.readAsDataURL(file)
    })
  }

  /**
   * 压缩图片：按 maxWidth 等比缩放，输出 JPEG
   * @param dataUrl 原始 base64 data URL
   * @param maxWidth 目标最大宽度（px）
   * @param quality JPEG 质量 0~1
   */
  function compressImage(dataUrl: string, maxWidth: number, quality: number): Promise<string> {
    return new Promise((resolve, reject) => {
      const img = new Image()
      img.onload = () => {
        const canvas = document.createElement('canvas')
        const ratio = Math.min(maxWidth / img.width, maxWidth / img.height, 1)
        canvas.width = img.width * ratio
        canvas.height = img.height * ratio
        const ctx = canvas.getContext('2d')
        if (!ctx) {
          // 降级返回原图
          resolve(dataUrl)
          return
        }
        ctx.fillStyle = '#FFFFFF'
        ctx.fillRect(0, 0, canvas.width, canvas.height)
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
        resolve(canvas.toDataURL('image/jpeg', quality))
      }
      img.onerror = () => reject(new Error('图片加载失败'))
      img.src = dataUrl
    })
  }

  /**
   * 确保 base64 字符串不超过阈值，否则多轮压缩降级
   * 第一轮：800px / 0.8 → 第二轮：480px / 0.6 → 仍超限则抛错
   */
  async function ensureBase64Limit(dataUrl: string): Promise<string> {
    if (dataUrl.length <= MAX_BASE64_LENGTH) {
      return dataUrl
    }
    // 第一轮压缩
    let compressed = await compressImage(dataUrl, 800, 0.8)
    if (compressed.length <= MAX_BASE64_LENGTH) {
      return compressed
    }
    // 第二轮更激进压缩
    compressed = await compressImage(compressed, 480, 0.6)
    if (compressed.length <= MAX_BASE64_LENGTH) {
      return compressed
    }
    throw new Error('图片压缩后仍超过大小限制，请更换图片')
  }

  return { fileToDataUrl, compressImage, ensureBase64Limit }
}
