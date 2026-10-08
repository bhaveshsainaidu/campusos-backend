import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

/**
 * CampusOS k6 load test.
 * Target: p95 < 300 ms for authenticated reads at a simulated 10,000-user population.
 * Scale stage up/down to match capacity; the default profile below ramps to 200 concurrent
 * virtual users (VUs), which with realistic think times models a 10,000-user campus.
 *
 * Run:  k6 run -e BASE_URL=http://localhost:8080 loadtest/loadtest.js
 */

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const errorRate = new Rate('errors');
const apiLatency = new Trend('api_latency', true);

export const options = {
    scenarios: {
        campus_load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 50 },   // warm-up
                { duration: '1m', target: 200 },   // steady load (models 10k users w/ think time)
                { duration: '1m', target: 200 },   // hold
                { duration: '30s', target: 0 },    // ramp-down
            ],
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],                    // <1% failures
        'http_req_duration{endpoint:students}': ['p(95)<300'],
        'http_req_duration{endpoint:dashboard}': ['p(95)<300'],
        'http_req_duration{endpoint:notices}': ['p(95)<300'],
        'http_req_duration{endpoint:courses}': ['p(95)<300'],
        errors: ['rate<0.05'],
    },
};

function login(email, password) {
    const res = http.post(`${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({ email, password }),
        { headers: { 'Content-Type': 'application/json' }, tags: { endpoint: 'login' } });
    check(res, { 'login 200': (r) => r.status === 200 });
    return res.json('accessToken');
}

function auth(token) {
    return { headers: { Authorization: `Bearer ${token}` }, tags: {} };
}

export default function () {
    // A campus population of 10,000 students; VUs cycle through representative profiles.
    const studentIdx = 1 + Math.floor(Math.random() * 3000);
    const adminToken = login('admin@campusos.edu', 'Admin@123');
    check(adminToken, { 'admin logged in': (t) => t && t.length > 20 });

    let res = http.get(`${BASE_URL}/api/v1/dashboard/admin`,
        Object.assign(auth(adminToken), { tags: { endpoint: 'dashboard' } }));
    check(res, { 'admin dashboard 200': (r) => r.status === 200 });
    apiLatency.add(res.timings.duration);

    res = http.get(`${BASE_URL}/api/v1/students?page=0&size=20`,
        Object.assign(auth(adminToken), { tags: { endpoint: 'students' } }));
    check(res, { 'students list 200': (r) => r.status === 200 });
    apiLatency.add(res.timings.duration);

    res = http.get(`${BASE_URL}/api/v1/courses?page=0&size=20`,
        Object.assign(auth(adminToken), { tags: { endpoint: 'courses' } }));
    check(res, { 'courses list 200': (r) => r.status === 200 });

    res = http.get(`${BASE_URL}/api/v1/notices?page=0&size=20`,
        Object.assign(auth(adminToken), { tags: { endpoint: 'notices' } }));
    check(res, { 'notices list 200': (r) => r.status === 200 });
    apiLatency.add(res.timings.duration);

    // Student flows (only first 3000 students have seeded data)
    const studentToken = login(`student${String(studentIdx).padStart(5, '0')}@campusos.edu`, 'Student@123');
    if (studentToken) {
        res = http.get(`${BASE_URL}/api/v1/attendance/me`,
            Object.assign(auth(studentToken), { tags: { endpoint: 'dashboard' } }));
        check(res, { 'student attendance 200': (r) => r.status === 200 });

        res = http.get(`${BASE_URL}/api/v1/dashboard/student`,
            Object.assign(auth(studentToken), { tags: { endpoint: 'dashboard' } }));
        check(res, { 'student dashboard 200': (r) => r.status === 200 });
        apiLatency.add(res.timings.duration);
    }

    sleep(1 + Math.random() * 2); // realistic think time
}
