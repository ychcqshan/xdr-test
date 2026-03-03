import request from './request'

export function getAlerts(params: any) {
    return request.get('/alerts', { params })
}

export function getAlertDetail(id: string) {
    return request.get(`/alerts/${id}`)
}

export function updateAlertStatus(id: string, data: any) {
    return request.put(`/alerts/${id}/status`, data)
}

export function respondToAlert(id: string, operator: string) {
    return request.post(`/alerts/${id}/respond`, null, { params: { operator } })
}

export function getAlertStats() {
    return request.get('/alerts/stats')
}
