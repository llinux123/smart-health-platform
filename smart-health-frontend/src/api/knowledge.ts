import request from './request'

// ============ 知识库管理（仅 ADMIN） ============

export interface KnowledgeDocument {
  id: string
  docId?: string
  title: string
  content: string
  category: string
  embedding?: number[]
}

export interface KnowledgePageResult {
  list: KnowledgeDocument[]
  total: number
  page: number
  size: number
}

export interface KnowledgeImportRequest {
  title: string
  content: string
  category: string
}

/** 导入医学知识 */
export function importKnowledge(data: KnowledgeImportRequest) {
  return request.post('/api/v1/admin/knowledge/import', data)
}

/** 分页查询知识库（支持关键字搜索） */
export function listKnowledge(page: number = 1, size: number = 10, keyword?: string) {
  return request.get('/api/v1/admin/knowledge', {
    params: { page, size, keyword }
  })
}

/** 删除知识库文档 */
export function deleteKnowledge(id: string) {
  return request.del(`/api/v1/admin/knowledge/${id}`)
}
