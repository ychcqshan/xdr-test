import request from './request'

export function getPolicies() {
    return request.get('/api/v1/policies')
}

export function savePolicy(data: any) {
    return request.post('/api/v1/policies', data)
}

export function getEffectivePolicy(agentId: string, groupId?: string) {
    return request.get(`/api/v1/policies/effective/${agentId}`, { params: { groupId } })
}
