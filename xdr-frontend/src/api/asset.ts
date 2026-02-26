import request from './request'

export function getAssets(params: any) {
    return request.get('/assets', { params })
}

export function getAssetDetail(id: string) {
    return request.get(`/assets/${id}`)
}

export function getAssetStats() {
    return request.get('/assets/stats')
}
