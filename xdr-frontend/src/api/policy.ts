import request from './request'

export function getPolicies() {
    return request.get('/policies')
}

export function savePolicy(data: any) {
    return request.post('/policies', data)
}

export function getEffectivePolicy(agentId: string, groupId?: string) {
    return request.get(`/policies/effective/${agentId}`, { params: { groupId } })
}

export function issueCommand(data: any) {
    return request.post('/policies/commands', data)
}
