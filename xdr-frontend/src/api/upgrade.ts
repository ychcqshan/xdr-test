import request from './request'

export function getUpgradePackages() {
    return request.get('/api/v1/upgrades/packages')
}

export function saveUpgradePackage(data: any) {
    return request.post('/api/v1/upgrades/packages', data)
}

export function getPendingUpgradeTask(agentId: string) {
    return request.get(`/api/v1/upgrades/tasks/pending/${agentId}`)
}
