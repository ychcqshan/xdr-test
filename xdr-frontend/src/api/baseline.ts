import request from './request'

export function getBaselines(params: any) {
    return request.get('/baselines', { params })
}

export function startLearning(data: { agentId: string, type: string, durationHours: number }) {
    return request.post('/baselines/learning', data)
}

export function approveBaseline(agentId: string, type: string) {
    return request.post('/baselines/approve', { agentId, type })
}

export function getBaselineItems(baselineId: string) {
    return request.get(`/baselines/${baselineId}/items`)
}
