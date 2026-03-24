import request from './request'

export function getComplianceStandards() {
    return request.get('/compliance/standards')
}

export function getComplianceResults(agentId: string) {
    return request.get(`/compliance/results/${agentId}`)
}
