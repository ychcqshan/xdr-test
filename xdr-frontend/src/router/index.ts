import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/login/LoginView.vue'),
        meta: { requiresAuth: false },
    },
    {
        path: '/',
        component: () => import('@/components/layout/AppLayout.vue'),
        redirect: '/dashboard',
        children: [
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: () => import('@/views/dashboard/DashboardView.vue'),
                meta: { title: '安全概览', icon: 'Monitor' },
            },
            {
                path: 'assets',
                name: 'Assets',
                component: () => import('@/views/asset/AssetListView.vue'),
                meta: { title: '资产管理', icon: 'Platform' },
            },
            {
                path: 'assets/:id',
                name: 'AssetDetail',
                component: () => import('@/views/asset/AssetDetailView.vue'),
                meta: { title: '资产详情', icon: 'Platform', hidden: true },
            },
            {
                path: 'alerts',
                name: 'Alerts',
                component: () => import('@/views/threat/AlertListView.vue'),
                meta: { title: '威胁告警', icon: 'Warning' },
            },
            {
                path: 'baselines',
                name: 'Baselines',
                component: () => import('@/views/baseline/BaselineListView.vue'),
                meta: { title: '基线管理', icon: 'Memo' },
            },
            {
                path: 'compliance',
                name: 'Compliance',
                component: () => import('@/views/compliance/ComplianceView.vue'),
                meta: { title: '合规管理', icon: 'Stamp' },
            },
            {
                path: 'policy',
                name: 'Policy',
                component: () => import('@/views/policy/PolicyView.vue'),
                meta: { title: '策略管理', icon: 'SetUp' },
            },
            {
                path: 'upgrade',
                name: 'Upgrade',
                component: () => import('@/views/upgrade/UpgradeView.vue'),
                meta: { title: '远程升级', icon: 'Upload' },
            },
            {
                path: 'users',
                name: 'Users',
                component: () => import('@/views/user/UserView.vue'),
                meta: { title: '用户管理', icon: 'User' },
            },
        ],
    },
]

const router = createRouter({
    history: createWebHistory(),
    routes,
})


// 路由守卫
router.beforeEach((to) => {
    const token = localStorage.getItem('accessToken')
    if (to.meta.requiresAuth !== false && !token) {
        return '/login'
    }
})

export default router
