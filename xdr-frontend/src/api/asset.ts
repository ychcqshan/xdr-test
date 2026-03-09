import request from './request'

export function getAssets(params: any) {
    return request.get('/assets', { params })
}

export function getAssetDetail(id: string) {
    return request.get(`/assets/${id}`)
}

export function getAssetDetails(agentId: string, startTime?: string, endTime?: string) {
    return request.get(`/assets/${agentId}/details`, { params: { startTime, endTime } })
}

export function getTopology() {
    return request.get('/assets/topology')
}

export function getAssetStats() {
    return request.get('/assets/stats')
}

export function getAssetTimeline(agentId: string, timestamp?: string) {
    return request.get(`/assets/${agentId}/timeline`, { params: { timestamp } })
}
