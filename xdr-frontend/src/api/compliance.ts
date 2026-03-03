import request from './request'

export function getComplianceStandards() {
    return request.get('/api/v1/compliance/standards')
}

export function getComplianceResults(agentId: string) {
    return request.get(`/api/v1/compliance/results/${agentId}`)
}
