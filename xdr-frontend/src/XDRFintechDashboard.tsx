import React, { useState } from 'react';
import {
    Shield,
    Activity,
    Terminal,
    Server,
    ArrowUpRight,
    ArrowDownRight,
    Bell,
    Search,
    ChevronRight,
    MoreHorizontal,
    Plus
} from 'lucide-react';
import {
    AreaChart,
    Area,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer
} from 'recharts';

/**
 * XDR Fintech Dashboard - Light Mode
 * Aesthetics: Stripe / Linear / Puzzle Fintech
 * Designed for: XDR Management Platform
 */

const data = [
    { name: '00:00', threats: 12 },
    { name: '04:00', threats: 8 },
    { name: '08:00', threats: 45 },
    { name: '12:00', threats: 25 },
    { name: '16:00', threats: 60 },
    { name: '20:00', threats: 32 },
    { name: '23:59', threats: 15 },
];

const StatCard = ({ label, value, trend, isPositive, icon: Icon }) => (
    <div className="group relative bg-white p-8 rounded-[24px] shadow-lg shadow-blue-500/5 hover:shadow-blue-500/10 transition-all duration-500 cursor-default">
        <div className="flex justify-between items-start mb-6">
            <div className="p-3 bg-slate-50 rounded-2xl group-hover:bg-blue-50 transition-colors duration-500">
                <Icon size={24} className="text-slate-400 group-hover:text-blue-500 transition-colors duration-500" />
            </div>
            <div className={`flex items-center gap-1 text-sm font-semibold px-2.5 py-1 rounded-full ${isPositive ? 'text-emerald-700 bg-emerald-50' : 'text-rose-700 bg-rose-50'
                }`}>
                {isPositive ? <ArrowUpRight size={14} /> : <ArrowDownRight size={14} />}
                {trend}%
            </div>
        </div>
        <p className="text-slate-500 text-sm font-medium mb-1">{label}</p>
        <h3 className="text-3xl font-bold text-slate-900 tracking-tight leading-none">{value}</h3>
    </div>
);

const ThreatBadge = ({ level }) => {
    const styles = {
        CRITICAL: 'bg-rose-50 text-rose-700 border-rose-100',
        HIGH: 'bg-amber-50 text-amber-700 border-amber-100',
        MEDIUM: 'bg-blue-50 text-blue-700 border-blue-100',
        LOW: 'bg-slate-50 text-slate-700 border-slate-100',
    };
    return (
        <span className={`px-3 py-1 rounded-full text-xs font-bold border ${styles[level]}`}>
            {level}
        </span>
    );
};

export default function XDRDashboard() {
    const [activeTab, setActiveTab] = useState('insights');

    return (
        <div className="min-h-screen bg-[#F8FAFC] text-slate-900 font-sans selection:bg-blue-100 selection:text-blue-700">
            {/* Navigation */}
            <nav className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b border-slate-100 px-8 h-16 flex items-center justify-between">
                <div className="flex items-center gap-8">
                    <div className="flex items-center gap-2">
                        <div className="w-8 h-8 bg-blue-600 rounded-xl flex items-center justify-center shadow-lg shadow-blue-600/20">
                            <span className="text-white flex items-center justify-center">
                                <Shield size={18} />
                            </span>
                        </div>
                        <span className="font-bold text-lg tracking-tight">XDR Console</span>
                    </div>
                    <div className="hidden md:flex gap-6 text-sm font-medium text-slate-500">
                        {['Dashboard', 'Assets', 'Threats', 'Settings'].map((item) => (
                            <a key={item} href="#" className="hover:text-slate-900 transition-colors">{item}</a>
                        ))}
                    </div>
                </div>
                <div className="flex items-center gap-4">
                    <button className="p-2 text-slate-400 hover:text-slate-900 transition-colors">
                        <Search size={20} />
                    </button>
                    <button className="relative p-2 text-slate-400 hover:text-slate-900 transition-colors">
                        <Bell size={20} />
                        <span className="absolute top-2 right-2 w-2 h-2 bg-blue-600 rounded-full border-2 border-white"></span>
                    </button>
                    <div className="w-8 h-8 rounded-full bg-slate-200 border-2 border-white shadow-sm overflow-hidden">
                        <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Felix" alt="Avatar" />
                    </div>
                </div>
            </nav>

            <main className="max-w-[1440px] mx-auto px-8 py-12">
                {/* Header Section */}
                <div className="flex flex-col md:flex-row justify-between items-end gap-6 mb-12">
                    <div>
                        <h1 className="text-4xl font-bold text-slate-900 tracking-tight mb-2">Good morning, Felix</h1>
                        <p className="text-slate-500 text-lg">System exposure is <span className="text-blue-600 font-semibold underline underline-offset-4 decoration-2">minimal</span>. No major threats detected in 24h.</p>
                    </div>
                    <button className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 px-6 rounded-[16px] shadow-lg shadow-blue-600/20 hover:shadow-blue-600/30 transition-all active:scale-95">
                        <Plus size={20} />
                        <span>Generate Report</span>
                    </button>
                </div>

                {/* Hero Stats */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 mb-16">
                    <StatCard label="Total Assets" value="2,842" trend="1.2" isPositive={true} icon={Server} />
                    <StatCard label="Active Threats" value="04" trend="15.4" isPositive={false} icon={Activity} />
                    <StatCard label="Blocked Attacks" value="1,240" trend="8.2" isPositive={true} icon={Shield} />
                    <StatCard label="Command Logs" value="84k" trend="2.4" isPositive={true} icon={Terminal} />
                </div>

                {/* Main Content Grid */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-12">
                    {/* Insights Chart - Spans 2 columns */}
                    <div className="lg:col-span-2 bg-white rounded-[32px] p-10 shadow-lg shadow-slate-200/50">
                        <div className="flex justify-between items-center mb-10">
                            <h2 className="text-2xl font-bold text-slate-900">Threat Insights</h2>
                            <div className="flex bg-slate-50 p-1 rounded-xl">
                                {['Daily', 'Weekly'].map((tab) => (
                                    <button
                                        key={tab}
                                        onClick={() => setActiveTab(tab.toLowerCase())}
                                        className={`px-4 py-1.5 rounded-lg text-sm font-semibold transition-all ${activeTab === tab.toLowerCase()
                                            ? 'bg-white text-slate-900 shadow-sm'
                                            : 'text-slate-500 hover:text-slate-900'
                                            }`}
                                    >
                                        {tab}
                                    </button>
                                ))}
                            </div>
                        </div>
                        <div className="h-[340px] w-full">
                            <ResponsiveContainer width="100%" height="100%">
                                <AreaChart data={data}>
                                    <defs>
                                        <linearGradient id="colorThreat" x1="0" y1="0" x2="0" y2="1">
                                            <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.15} />
                                            <stop offset="95%" stopColor="#3B82F6" stopOpacity={0} />
                                        </linearGradient>
                                    </defs>
                                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F1F5F9" />
                                    <XAxis
                                        dataKey="name"
                                        axisLine={false}
                                        tickLine={false}
                                        tick={{ fill: '#94A3B8', fontSize: 12 }}
                                        dy={16}
                                    />
                                    <YAxis hide />
                                    <Tooltip
                                        contentStyle={{ borderRadius: '16px', border: 'none', boxShadow: '0 10px 40px -10px rgba(0,0,0,0.1)', padding: '12px' }}
                                        cursor={{ stroke: '#3B82F6', strokeWidth: 2 }}
                                    />
                                    <Area
                                        type="monotone"
                                        dataKey="threats"
                                        stroke="#3B82F6"
                                        strokeWidth={4}
                                        fillOpacity={1}
                                        fill="url(#colorThreat)"
                                    />
                                </AreaChart>
                            </ResponsiveContainer>
                        </div>
                    </div>

                    {/* Recent Feed - Spans 1 column */}
                    <div className="bg-white rounded-[32px] p-10 shadow-lg shadow-slate-200/50">
                        <div className="flex justify-between items-center mb-8">
                            <h2 className="text-xl font-bold text-slate-900">Threat Feed</h2>
                            <button className="text-blue-600 hover:bg-blue-50 p-2 rounded-lg transition-colors">
                                <ChevronRight size={20} />
                            </button>
                        </div>
                        <div className="space-y-6">
                            {[
                                { title: 'SSH Brute Force', target: 'Server-04', level: 'HIGH', time: '2m ago' },
                                { title: 'Unusual Port Scan', target: 'Workstation-X', level: 'MEDIUM', time: '14m ago' },
                                { title: 'Honeyfile Modified', target: 'Marketing-PC', level: 'CRITICAL', time: '1h ago' },
                                { title: 'Malicious Command', target: 'Dev-01', level: 'HIGH', time: '3h ago' },
                                { title: 'Root Privileges Access', target: 'Core-Router', level: 'CRITICAL', time: '5h ago' },
                            ].map((item, i) => (
                                <div key={i} className="flex flex-col gap-2 pb-6 border-b border-slate-50 last:border-0 last:pb-0 group cursor-pointer">
                                    <div className="flex justify-between items-start">
                                        <h4 className="font-bold text-slate-900 group-hover:text-blue-600 transition-colors">{item.title}</h4>
                                        <span className="text-xs text-slate-400 font-medium">{item.time}</span>
                                    </div>
                                    <div className="flex justify-between items-center">
                                        <span className="text-sm text-slate-500 font-medium">on {item.target}</span>
                                        <ThreatBadge level={item.level} />
                                    </div>
                                </div>
                            ))}
                        </div>
                        <button className="w-full mt-10 py-4 bg-slate-50 hover:bg-slate-100 text-slate-600 font-bold text-sm rounded-2xl transition-all">
                            View All Alerts
                        </button>
                    </div>
                </div>

                {/* Assets Preview Section */}
                <section className="mt-20">
                    <div className="flex justify-between items-center mb-8 px-2">
                        <div>
                            <h2 className="text-2xl font-bold text-slate-900 mb-1">Endpoints Monitor</h2>
                            <p className="text-slate-500 text-sm font-medium">Monitoring 2,842 active agents worldwide</p>
                        </div>
                        <button className="px-4 py-2 text-sm font-bold text-slate-900 hover:bg-slate-100 rounded-xl transition-colors">Manage All</button>
                    </div>

                    <div className="bg-white rounded-[32px] overflow-hidden shadow-lg shadow-slate-200/50">
                        <table className="w-full text-left border-collapse">
                            <thead>
                                <tr className="bg-slate-50/50">
                                    <th className="px-8 py-5 text-xs font-bold text-slate-400 uppercase tracking-widest">Hostname</th>
                                    <th className="px-8 py-5 text-xs font-bold text-slate-400 uppercase tracking-widest">IP Address</th>
                                    <th className="px-8 py-5 text-xs font-bold text-slate-400 uppercase tracking-widest">OS Type</th>
                                    <th className="px-8 py-5 text-xs font-bold text-slate-400 uppercase tracking-widest">Health</th>
                                    <th className="px-8 py-5 text-xs font-bold text-slate-400 uppercase tracking-widest">Events</th>
                                    <th className="px-8 py-5"></th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-50 text-sm font-medium">
                                {[
                                    { host: 'PROD-SERVER-01', ip: '10.0.0.142', os: 'Linux / Ubuntu', health: 98, events: 12 },
                                    { host: 'MARKETING-WIN-10', ip: '192.168.1.55', os: 'Windows 10', health: 82, events: 45 },
                                    { host: 'CEO-MACBOOK-PRO', ip: '10.0.2.14', os: 'macOS Monterey', health: 100, events: 2 },
                                    { host: 'DEVELOPER-WS-09', ip: '172.16.0.4', os: 'Linux / Arch', health: 94, events: 8 },
                                ].map((row, i) => (
                                    <tr key={i} className="hover:bg-slate-50 transition-colors group">
                                        <td className="px-8 py-6 text-slate-900 font-bold">{row.host}</td>
                                        <td className="px-8 py-6 text-slate-500">{row.ip}</td>
                                        <td className="px-8 py-6 text-slate-500">{row.os}</td>
                                        <td className="px-8 py-6">
                                            <div className="flex items-center gap-3">
                                                <div className="w-24 h-1.5 bg-slate-100 rounded-full overflow-hidden">
                                                    <div
                                                        className={`h-full rounded-full ${row.health > 90 ? 'bg-emerald-500' : 'bg-amber-500'}`}
                                                        style={{ width: `${row.health}%` }}
                                                    />
                                                </div>
                                                <span className="text-xs text-slate-900">{row.health}%</span>
                                            </div>
                                        </td>
                                        <td className="px-8 py-6">
                                            <span className="bg-slate-100 px-2 py-1 rounded text-xs text-slate-600">{row.events} today</span>
                                        </td>
                                        <td className="px-8 py-6 text-right opacity-0 group-hover:opacity-100 transition-opacity">
                                            <button className="p-2 text-slate-400 hover:text-slate-900">
                                                <MoreHorizontal size={20} />
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </section>
            </main>
        </div>
    );
}
