import request from './request'

export function getUpgradePackages() {
    return request.get('/upgrades/packages')
}

export function saveUpgradePackage(data: any) {
    return request.post('/upgrades/packages', data)
}

export function getPendingUpgradeTask(agentId: string) {
    return request.get(`/upgrades/tasks/pending/${agentId}`)
}
