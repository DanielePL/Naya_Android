"""
Admin Dashboard Router - Comprehensive cost tracking, analytics, and partner management
"""

from fastapi import APIRouter, HTTPException, Request, Response
from fastapi.responses import HTMLResponse, RedirectResponse
from typing import Optional
import os
import hashlib
import secrets
from datetime import datetime, timedelta

router = APIRouter(tags=["Admin"])

# Legacy password support (will be deprecated)
ADMIN_PASSWORD = os.environ.get("ADMIN_PASSWORD", "naya_admin_2024")

# Authorized admin users (user_id -> email)
AUTHORIZED_ADMINS = {
    "faba7636-66b9-43cd-8570-37cdc32ffff0": "danielepauli@gmail.com",
    "b63ad00f-95c7-4ca3-9f6f-1b281f5c78a7": "ks.k.kaenel@gmail.com",
    "5f4afbeb-0b0e-46c7-9caf-0d95c57fbd93": "kloe.borge18@gmail.com",  # Chloe - App Owner
}

# Session storage (in production, use Redis or database)
admin_sessions = {}  # session_token -> {user_id, email, expires_at}


def generate_session_token() -> str:
    """Generate a secure session token"""
    return secrets.token_urlsafe(32)


def create_admin_session(user_id: str, email: str) -> str:
    """Create a new admin session"""
    token = generate_session_token()
    admin_sessions[token] = {
        "user_id": user_id,
        "email": email,
        "expires_at": datetime.now() + timedelta(hours=24)
    }
    return token


def verify_session(token: str) -> Optional[dict]:
    """Verify session token and return session data if valid"""
    if not token:
        return None
    session = admin_sessions.get(token)
    if not session:
        return None
    if datetime.now() > session["expires_at"]:
        del admin_sessions[token]
        return None
    return session


def verify_admin(password: str) -> bool:
    """Verify admin password (legacy support)"""
    return password == ADMIN_PASSWORD


def verify_admin_user(user_id: str) -> bool:
    """Verify if user_id is an authorized admin"""
    return user_id in AUTHORIZED_ADMINS


def get_supabase_client():
    """Get Supabase client"""
    from supabase import create_client
    supabase_url = os.environ.get("SUPABASE_URL")
    supabase_key = os.environ.get("SUPABASE_KEY")
    return create_client(supabase_url, supabase_key)


# ============================================================
# ADMIN DASHBOARD HTML
# ============================================================

ADMIN_LOGIN_HTML = """
<!DOCTYPE html>
<html>
<head>
    <title>Naya Admin - Login</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
               display: flex; justify-content: center; align-items: center;
               min-height: 100vh; margin: 0; background: #0f0f0f; color: #fff; }
        .login-box { background: #1a1a1a; padding: 40px; border-radius: 12px;
                     box-shadow: 0 4px 20px rgba(0,0,0,0.3); max-width: 400px; width: 90%; }
        h1 { color: #ff6b35; margin-bottom: 10px; }
        .subtitle { color: #666; font-size: 14px; margin-bottom: 30px; }
        .form-group { margin-bottom: 15px; }
        label { display: block; color: #888; font-size: 12px; margin-bottom: 5px; text-transform: uppercase; }
        input { padding: 12px 16px; font-size: 16px; border: 1px solid #333;
                border-radius: 8px; background: #2a2a2a; color: #fff; width: 100%; box-sizing: border-box; }
        input:focus { border-color: #ff6b35; outline: none; }
        button { padding: 12px 24px; font-size: 16px; background: #ff6b35;
                 color: #fff; border: none; border-radius: 8px; cursor: pointer;
                 margin-top: 10px; width: 100%; }
        button:hover { background: #ff8555; }
        button:disabled { background: #444; cursor: not-allowed; }
        .error { color: #ef4444; font-size: 14px; margin-top: 15px; display: none; }
        .divider { border-top: 1px solid #333; margin: 25px 0; position: relative; }
        .divider span { position: absolute; top: -10px; left: 50%; transform: translateX(-50%);
                        background: #1a1a1a; padding: 0 10px; color: #666; font-size: 12px; }
        .legacy-login { margin-top: 20px; }
        .legacy-login summary { color: #666; font-size: 13px; cursor: pointer; }
        .legacy-login summary:hover { color: #888; }
        .legacy-form { margin-top: 15px; }
    </style>
</head>
<body>
    <div class="login-box">
        <h1>Naya Admin</h1>
        <p class="subtitle">Secure access for authorized administrators only</p>

        <form id="loginForm" onsubmit="handleLogin(event)">
            <div class="form-group">
                <label>Email</label>
                <input type="email" id="email" placeholder="admin@example.com" required>
            </div>
            <div class="form-group">
                <label>User ID</label>
                <input type="text" id="userId" placeholder="Your Supabase User ID" required>
            </div>
            <button type="submit" id="loginBtn">Login</button>
            <div class="error" id="error"></div>
        </form>

        <div class="divider"><span>OR</span></div>

        <details class="legacy-login">
            <summary>Legacy Password Login</summary>
            <form class="legacy-form" method="get">
                <div class="form-group">
                    <label>Admin Password</label>
                    <input type="password" name="password" placeholder="Enter password">
                </div>
                <button type="submit">Login with Password</button>
            </form>
        </details>
    </div>

    <script>
        async function handleLogin(e) {
            e.preventDefault();
            const btn = document.getElementById('loginBtn');
            const error = document.getElementById('error');
            const email = document.getElementById('email').value;
            const userId = document.getElementById('userId').value;

            btn.disabled = true;
            btn.textContent = 'Logging in...';
            error.style.display = 'none';

            try {
                const res = await fetch('/api/v1/admin/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, user_id: userId })
                });

                const data = await res.json();

                if (data.success && data.session_token) {
                    // Store session and redirect
                    window.location.href = '/admin?session=' + data.session_token;
                } else {
                    error.textContent = data.error || 'Login failed. Check your credentials.';
                    error.style.display = 'block';
                }
            } catch (err) {
                error.textContent = 'Connection error. Please try again.';
                error.style.display = 'block';
            }

            btn.disabled = false;
            btn.textContent = 'Login';
        }
    </script>
</body>
</html>
"""


def get_admin_dashboard_html(password: str) -> str:
    """Generate admin dashboard HTML with embedded password"""
    return f"""
    <!DOCTYPE html>
    <html>
    <head>
        <title>Naya Admin Dashboard</title>
        <style>
            * {{ box-sizing: border-box; }}
            body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                   margin: 0; padding: 20px; background: #0f0f0f; color: #e0e0e0; }}
            .header {{ display: flex; justify-content: space-between; align-items: center;
                      margin-bottom: 30px; padding-bottom: 20px; border-bottom: 1px solid #333; }}
            h1 {{ color: #ff6b35; margin: 0; }}
            .refresh-btn {{ background: #333; color: #fff; border: none; padding: 10px 20px;
                           border-radius: 8px; cursor: pointer; }}
            .refresh-btn:hover {{ background: #444; }}
            .grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                    gap: 15px; margin-bottom: 25px; }}
            .card {{ background: #1a1a1a; border-radius: 12px; padding: 20px;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.2); }}
            .card.highlight {{ border: 2px solid #ff6b35; }}
            .card.green {{ border-left: 4px solid #4ade80; }}
            .card.red {{ border-left: 4px solid #ef4444; }}
            .card.blue {{ border-left: 4px solid #60a5fa; }}
            .card.orange {{ border-left: 4px solid #fb923c; }}
            .card.purple {{ border-left: 4px solid #a855f7; }}
            .card h2 {{ color: #ff6b35; margin-top: 0; font-size: 14px; text-transform: uppercase; }}
            .card h3 {{ color: #888; margin: 15px 0 10px 0; font-size: 13px; }}
            .stat {{ font-size: 28px; font-weight: bold; color: #fff; }}
            .stat.small {{ font-size: 22px; }}
            .stat-label {{ color: #666; font-size: 12px; margin-top: 4px; }}
            .profit {{ color: #4ade80; }}
            .loss {{ color: #ef4444; }}
            .table {{ width: 100%; border-collapse: collapse; font-size: 14px; }}
            .table th, .table td {{ padding: 10px 12px; text-align: left; border-bottom: 1px solid #333; }}
            .table th {{ color: #ff6b35; font-weight: 600; font-size: 12px; text-transform: uppercase; }}
            .table tr:hover {{ background: #222; }}
            .cost {{ color: #4ade80; }}
            .tabs {{ display: flex; gap: 8px; margin-bottom: 20px; flex-wrap: wrap; }}
            .tab {{ padding: 8px 16px; background: #222; border: none; color: #888;
                   border-radius: 8px; cursor: pointer; font-size: 13px; }}
            .tab.active {{ background: #ff6b35; color: #fff; }}
            .section {{ margin-bottom: 30px; }}
            .section-title {{ color: #ff6b35; font-size: 18px; margin-bottom: 15px;
                             padding-bottom: 10px; border-bottom: 1px solid #333; }}
            .form-row {{ display: flex; gap: 10px; margin-bottom: 10px; flex-wrap: wrap; }}
            .form-row input, .form-row select {{ padding: 10px 12px; background: #222; border: 1px solid #333;
                                                 border-radius: 6px; color: #fff; font-size: 14px; }}
            .form-row input {{ flex: 1; min-width: 150px; }}
            .form-row select {{ min-width: 140px; }}
            .btn {{ padding: 10px 20px; background: #ff6b35; color: #fff; border: none;
                   border-radius: 6px; cursor: pointer; font-size: 14px; }}
            .btn:hover {{ background: #ff8555; }}
            .btn.secondary {{ background: #333; }}
            .btn.danger {{ background: #ef4444; }}
            .badge {{ display: inline-block; padding: 4px 10px; border-radius: 12px;
                     font-size: 11px; font-weight: 600; }}
            .badge.below {{ background: #064e3b; color: #4ade80; }}
            .badge.average {{ background: #3f3f00; color: #fbbf24; }}
            .badge.high {{ background: #450a0a; color: #ef4444; }}
        </style>
    </head>
    <body>
        <div class="header">
            <h1>Naya Cost Analytics</h1>
            <button class="refresh-btn" onclick="loadAllData()">Refresh All</button>
        </div>

        <div class="section">
            <div class="section-title">Comprehensive Monthly Summary</div>
            <div class="grid">
                <div class="card highlight">
                    <h2>Total Monthly Cost</h2>
                    <div class="stat" id="total-monthly-cost">$0.00</div>
                    <div class="stat-label">Variable + Fixed Costs</div>
                </div>
                <div class="card green">
                    <h2>Variable Costs (API)</h2>
                    <div class="stat small" id="variable-costs">$0.00</div>
                    <div class="stat-label">OpenAI, VBT, Storage</div>
                </div>
                <div class="card orange">
                    <h2>Fixed Costs</h2>
                    <div class="stat small" id="fixed-costs">$0.00</div>
                    <div class="stat-label">Labor, Subscriptions, Infra</div>
                </div>
                <div class="card blue">
                    <h2>Active Users</h2>
                    <div class="stat small" id="active-users">0</div>
                    <div class="stat-label">This month</div>
                </div>
                <div class="card highlight">
                    <h2>Cost Per User</h2>
                    <div class="stat" id="cost-per-user">$0.00</div>
                    <div class="stat-label">Total cost / active users</div>
                </div>
                <div class="card">
                    <h2>Industry Comparison</h2>
                    <div class="stat small" id="industry-avg">$0.70</div>
                    <div class="stat-label">Avg fitness app cost/user</div>
                    <div style="margin-top: 10px;">
                        <span class="badge below" id="cost-badge">Calculating...</span>
                    </div>
                </div>
            </div>
        </div>

        <div class="section">
            <div class="section-title">Variable Costs by Service</div>
            <div class="grid">
                <div class="card purple">
                    <h2>AI Coach</h2>
                    <div class="stat small" id="ai-coach-cost">$0.00</div>
                    <div class="stat-label" id="ai-coach-calls">0 messages</div>
                </div>
                <div class="card blue">
                    <h2>Photo Analysis</h2>
                    <div class="stat small" id="photo-analysis-cost">$0.00</div>
                    <div class="stat-label" id="photo-analysis-calls">0 scans</div>
                </div>
                <div class="card orange">
                    <h2>VBT Analysis</h2>
                    <div class="stat small" id="vbt-cost">$0.00</div>
                    <div class="stat-label" id="vbt-calls">0 videos</div>
                </div>
                <div class="card">
                    <h2>Storage</h2>
                    <div class="stat small" id="storage-cost">$0.00</div>
                    <div class="stat-label" id="storage-calls">0 uploads</div>
                </div>
            </div>
        </div>

        <div class="section">
            <div class="section-title">Revenue & App Store Commissions</div>
            <div class="grid">
                <div class="card green">
                    <h2>Gross Revenue</h2>
                    <div class="stat small" id="gross-revenue">$0.00</div>
                    <div class="stat-label">Before app store cut</div>
                </div>
                <div class="card red">
                    <h2>App Store Commission</h2>
                    <div class="stat small" id="app-store-commission">$0.00</div>
                    <div class="stat-label">Small Biz Program: 15%</div>
                </div>
                <div class="card green">
                    <h2>Net Revenue</h2>
                    <div class="stat small" id="net-revenue">$0.00</div>
                    <div class="stat-label">After commission</div>
                </div>
                <div class="card" id="profit-card">
                    <h2>Monthly Profit/Loss</h2>
                    <div class="stat small profit" id="monthly-profit">$0.00</div>
                    <div class="stat-label">Net revenue - total costs</div>
                </div>
            </div>
        </div>

        <div class="section">
            <div class="section-title">Break-Even Analysis</div>
            <div class="grid">
                <div class="card highlight" style="grid-column: span 2;">
                    <h2>Subscribers Needed for Break-Even</h2>
                    <div style="display: flex; gap: 30px; margin-top: 15px;">
                        <div style="flex: 1; text-align: center; padding: 15px; background: #222; border-radius: 8px;">
                            <div style="color: #888; font-size: 12px; margin-bottom: 5px;">MONTHLY SUBS ONLY</div>
                            <div class="stat" id="be-monthly-users" style="color: #60a5fa;">0</div>
                            <div class="stat-label">@ $<span id="be-monthly-price">9.99</span>/mo net</div>
                        </div>
                        <div style="flex: 1; text-align: center; padding: 15px; background: #222; border-radius: 8px;">
                            <div style="color: #888; font-size: 12px; margin-bottom: 5px;">YEARLY SUBS ONLY</div>
                            <div class="stat" id="be-yearly-users" style="color: #4ade80;">0</div>
                            <div class="stat-label">@ $<span id="be-yearly-price">59.99</span>/yr net</div>
                        </div>
                        <div style="flex: 1; text-align: center; padding: 15px; background: #333; border-radius: 8px; border: 1px solid #ff6b35;">
                            <div style="color: #ff6b35; font-size: 12px; margin-bottom: 5px;">REALISTIC MIX (65/35)</div>
                            <div class="stat" id="be-mixed-users" style="color: #ff6b35;">0</div>
                            <div class="stat-label">65% monthly / 35% yearly</div>
                        </div>
                    </div>
                </div>
                <div class="card">
                    <h2>Monthly Costs to Cover</h2>
                    <div style="margin-top: 10px;">
                        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                            <span class="stat-label">Fixed Costs:</span>
                            <span>$<span id="be-fixed">0</span></span>
                        </div>
                        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                            <span class="stat-label">Variable Costs/User:</span>
                            <span>$<span id="be-variable">0.05</span></span>
                        </div>
                        <div style="display: flex; justify-content: space-between; padding-top: 8px; border-top: 1px solid #333;">
                            <span style="font-weight: bold;">Total Monthly:</span>
                            <span style="font-weight: bold; color: #ff6b35;">$<span id="be-total-monthly">0</span></span>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="tabs">
            <button class="tab active" onclick="showTab('daily')">Daily Costs</button>
            <button class="tab" onclick="showTab('users')">Per-User Costs</button>
            <button class="tab" onclick="showTab('events')">Event Types</button>
            <button class="tab" onclick="showTab('fixed')">Fixed Costs</button>
            <button class="tab" onclick="showTab('salaries')">Salaries</button>
            <button class="tab" onclick="showTab('revenue')">Revenue</button>
        </div>

        <div class="card">
            <div id="daily-tab">
                <h2>Daily Variable Cost Breakdown</h2>
                <table class="table">
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>OpenAI Vision</th>
                            <th>VBT Analysis</th>
                            <th>AI Coach</th>
                            <th>Storage</th>
                            <th>Total Cost</th>
                        </tr>
                    </thead>
                    <tbody id="daily-table">
                        <tr><td colspan="6">Loading data...</td></tr>
                    </tbody>
                </table>
            </div>

            <div id="users-tab" style="display:none;">
                <h2>Per-User Costs (Lifetime)</h2>
                <table class="table">
                    <thead>
                        <tr>
                            <th>User ID</th>
                            <th>First Activity</th>
                            <th>Active Days</th>
                            <th>Total Events</th>
                            <th>Total Cost</th>
                        </tr>
                    </thead>
                    <tbody id="users-table">
                        <tr><td colspan="5">Loading...</td></tr>
                    </tbody>
                </table>
            </div>

            <div id="events-tab" style="display:none;">
                <h2>Event Type Breakdown (This Month)</h2>
                <table class="table">
                    <thead>
                        <tr>
                            <th>Event Type</th>
                            <th>Count</th>
                            <th>Avg Tokens</th>
                            <th>Total Cost</th>
                        </tr>
                    </thead>
                    <tbody id="events-table">
                        <tr><td colspan="4">Loading...</td></tr>
                    </tbody>
                </table>
            </div>

            <div id="fixed-tab" style="display:none;">
                <h2>Fixed Monthly Costs</h2>
                <div class="form-row" style="margin-bottom: 20px;">
                    <input type="text" id="fc-name" placeholder="Name (e.g., Claude Max)">
                    <select id="fc-category">
                        <option value="subscription">Subscription</option>
                        <option value="labor">Labor</option>
                        <option value="infrastructure">Infrastructure</option>
                        <option value="marketing">Marketing</option>
                        <option value="app_store_fees">App Store Fees</option>
                        <option value="other">Other</option>
                    </select>
                    <input type="number" id="fc-amount" placeholder="Amount ($)" step="0.01">
                    <select id="fc-recurrence">
                        <option value="monthly">Monthly</option>
                        <option value="yearly">Yearly</option>
                        <option value="one_time">One-time</option>
                    </select>
                    <button class="btn" onclick="addFixedCost()">+ Add Cost</button>
                </div>
                <table class="table">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Category</th>
                            <th>Amount</th>
                            <th>Recurrence</th>
                            <th>Monthly Equiv.</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody id="fixed-table">
                        <tr><td colspan="6">Loading...</td></tr>
                    </tbody>
                </table>
            </div>

            <div id="salaries-tab" style="display:none;">
                <h2>Employee Salaries (Variable - Based on Net Revenue)</h2>
                <div style="background: #222; padding: 15px; border-radius: 8px; margin-bottom: 20px;">
                    <div style="color: #ff6b35; font-size: 14px; margin-bottom: 10px;">Salary Calculation Formula</div>
                    <div style="color: #888; font-size: 13px; line-height: 1.8;">
                        <strong style="color: #4ade80;">Net Available</strong> = Gross Revenue - Operating Costs - 15% Reserve<br>
                        <strong style="color: #60a5fa;">Employee Salary</strong> = Net Available × Revenue Share %
                    </div>
                </div>
                <div style="background: #1a3a1a; padding: 15px; border-radius: 8px; margin-bottom: 20px; border: 1px solid #2a5a2a;">
                    <h3 style="color: #4ade80; margin: 0 0 10px 0;">Current Month Calculation</h3>
                    <div style="display: grid; grid-template-columns: repeat(5, 1fr); gap: 15px; text-align: center;">
                        <div>
                            <div style="color: #888; font-size: 11px;">GROSS REVENUE</div>
                            <div style="color: #fff; font-size: 18px; font-weight: bold;" id="sal-gross">$0.00</div>
                        </div>
                        <div>
                            <div style="color: #888; font-size: 11px;">OPERATING COSTS</div>
                            <div style="color: #ef4444; font-size: 18px; font-weight: bold;" id="sal-costs">-$0.00</div>
                        </div>
                        <div>
                            <div style="color: #888; font-size: 11px;">RESERVE (15%)</div>
                            <div style="color: #fb923c; font-size: 18px; font-weight: bold;" id="sal-reserve">-$0.00</div>
                        </div>
                        <div>
                            <div style="color: #888; font-size: 11px;">NET AVAILABLE</div>
                            <div style="color: #4ade80; font-size: 18px; font-weight: bold;" id="sal-net">$0.00</div>
                        </div>
                        <div>
                            <div style="color: #888; font-size: 11px;">TOTAL SALARIES</div>
                            <div style="color: #60a5fa; font-size: 18px; font-weight: bold;" id="sal-total">$0.00</div>
                        </div>
                    </div>
                </div>
                <div style="margin-bottom: 20px; padding: 15px; background: #1a1a1a; border-radius: 8px; border: 1px solid #333;">
                    <div style="color: #ff6b35; font-size: 14px; margin-bottom: 10px;">Add Employee</div>
                    <div class="form-row">
                        <input type="text" id="emp-name" placeholder="Name" style="flex: 2;">
                        <input type="text" id="emp-role" placeholder="Role (e.g., Developer)" style="flex: 1.5;">
                        <input type="number" id="emp-base-salary" placeholder="Base Salary ($)" step="0.01" style="flex: 1;">
                        <input type="number" id="emp-share" placeholder="Revenue Share %" step="0.01" min="0" max="100" style="flex: 1;">
                        <button class="btn" onclick="addEmployee()">+ Add Employee</button>
                    </div>
                </div>
                <table class="table">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Role</th>
                            <th>Base Salary (Target)</th>
                            <th>Revenue Share %</th>
                            <th>Calculated Salary</th>
                            <th>% of Target</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody id="salaries-table">
                        <tr><td colspan="7">Loading...</td></tr>
                    </tbody>
                </table>
            </div>

            <div id="revenue-tab" style="display:none;">
                <h2>Revenue Tracking</h2>
                <div style="display: flex; gap: 15px; align-items: center; margin-bottom: 20px; padding: 15px; background: #222; border-radius: 8px;">
                    <label style="color: #888;">Filter by Month:</label>
                    <input type="month" id="rev-month-filter" style="padding: 8px 12px; background: #333; border: 1px solid #444; border-radius: 6px; color: #fff;">
                    <button class="btn secondary" onclick="loadRevenue()" style="padding: 8px 16px;">Show All</button>
                    <button class="btn" onclick="filterRevenueByMonth()" style="padding: 8px 16px;">Filter</button>
                </div>
                <div id="monthly-summary" style="display: none; margin-bottom: 20px; padding: 20px; background: linear-gradient(135deg, #1a3a1a 0%, #1a1a1a 100%); border-radius: 8px; border: 1px solid #2a5a2a;">
                    <h3 style="color: #4ade80; margin: 0 0 15px 0;">Monthly Summary: <span id="summary-month">-</span></h3>
                    <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px;">
                        <div style="text-align: center;">
                            <div style="color: #888; font-size: 12px;">GROSS REVENUE</div>
                            <div style="color: #fff; font-size: 24px; font-weight: bold;" id="summary-gross">$0.00</div>
                        </div>
                        <div style="text-align: center;">
                            <div style="color: #888; font-size: 12px;">APP STORE FEE</div>
                            <div style="color: #ef4444; font-size: 24px; font-weight: bold;" id="summary-commission">-$0.00</div>
                        </div>
                        <div style="text-align: center;">
                            <div style="color: #888; font-size: 12px;">NET REVENUE</div>
                            <div style="color: #4ade80; font-size: 24px; font-weight: bold;" id="summary-net">$0.00</div>
                        </div>
                        <div style="text-align: center;">
                            <div style="color: #888; font-size: 12px;">TRANSACTIONS</div>
                            <div style="color: #60a5fa; font-size: 24px; font-weight: bold;" id="summary-count">0</div>
                        </div>
                    </div>
                </div>
                <div style="margin-bottom: 20px; padding: 15px; background: #1a1a1a; border-radius: 8px; border: 1px solid #333;">
                    <div style="color: #ff6b35; font-size: 14px; margin-bottom: 10px;">Add New Revenue Entry</div>
                    <div class="form-row">
                        <select id="rev-platform">
                            <option value="ios">iOS (Apple)</option>
                            <option value="android">Android (Google)</option>
                            <option value="web">Web (No Commission)</option>
                        </select>
                        <select id="rev-type">
                            <option value="subscription">Subscription</option>
                            <option value="one_time_purchase">One-time Purchase</option>
                            <option value="ad_revenue">Ad Revenue</option>
                        </select>
                        <input type="number" id="rev-amount" placeholder="Gross Revenue ($)" step="0.01">
                        <input type="date" id="rev-date" title="Revenue Date">
                        <button class="btn" onclick="addRevenue()">+ Add Revenue</button>
                    </div>
                </div>
                <table class="table">
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>Platform</th>
                            <th>Type</th>
                            <th>Gross Revenue</th>
                            <th>Commission (15%)</th>
                            <th>Net Revenue</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody id="revenue-table">
                        <tr><td colspan="7">Loading...</td></tr>
                    </tbody>
                </table>
            </div>
        </div>

        <div class="card" style="margin-top: 20px;">
            <h2>Current Cost Rates</h2>
            <div class="grid" style="grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));">
                <div>
                    <h3>OpenAI GPT-4o Vision</h3>
                    <div class="stat-label">$2.50/1M input tokens</div>
                    <div class="stat-label">$10/1M output tokens</div>
                    <div class="stat-label">~$0.01-0.03 per meal photo</div>
                </div>
                <div>
                    <h3>Claude 3 Haiku</h3>
                    <div class="stat-label">$0.25/1M input tokens</div>
                    <div class="stat-label">$1.25/1M output tokens</div>
                </div>
                <div>
                    <h3>App Store Commissions</h3>
                    <div class="stat-label">Apple: 30% (15% small biz)</div>
                    <div class="stat-label">Google: 30% (15% small biz)</div>
                </div>
                <div>
                    <h3>Industry Benchmarks</h3>
                    <div class="stat-label">Avg Infra: $0.50/user/mo</div>
                    <div class="stat-label">Avg Support: $0.20/user/mo</div>
                </div>
            </div>
        </div>

        <script>
            const password = '{password}';

            async function loadAllData() {{
                await Promise.all([
                    loadSummary(),
                    loadComprehensiveSummary(),
                    loadBreakEven(),
                    loadServiceCosts(),
                    loadDailyData(),
                    loadUsersData(),
                    loadEventsData(),
                    loadFixedCosts(),
                    loadSalaries(),
                    loadRevenue()
                ]);
            }}

            async function loadServiceCosts() {{
                try {{
                    const res = await fetch(`/api/v1/admin/service-costs?password=${{password}}`);
                    const data = await res.json();

                    // AI Coach
                    document.getElementById('ai-coach-cost').textContent = '$' + (data.ai_coach?.cost || 0).toFixed(2);
                    document.getElementById('ai-coach-calls').textContent = (data.ai_coach?.count || 0) + ' messages';

                    // Photo Analysis
                    document.getElementById('photo-analysis-cost').textContent = '$' + (data.photo_analysis?.cost || 0).toFixed(2);
                    document.getElementById('photo-analysis-calls').textContent = (data.photo_analysis?.count || 0) + ' scans';

                    // VBT Analysis
                    document.getElementById('vbt-cost').textContent = '$' + (data.vbt?.cost || 0).toFixed(2);
                    document.getElementById('vbt-calls').textContent = (data.vbt?.count || 0) + ' videos';

                    // Storage
                    document.getElementById('storage-cost').textContent = '$' + (data.storage?.cost || 0).toFixed(2);
                    document.getElementById('storage-calls').textContent = (data.storage?.count || 0) + ' uploads';
                }} catch (e) {{ console.error('Service costs error:', e); }}
            }}

            async function loadBreakEven() {{
                try {{
                    const res = await fetch(`/api/v1/admin/break-even?password=${{password}}`);
                    const data = await res.json();
                    document.getElementById('be-monthly-users').textContent = data.monthly_subs_needed || 0;
                    document.getElementById('be-yearly-users').textContent = data.yearly_subs_needed || 0;
                    document.getElementById('be-mixed-users').textContent = data.mixed_subs_needed || 0;
                    document.getElementById('be-monthly-price').textContent = (data.monthly_net_price || 6.99).toFixed(2);
                    document.getElementById('be-yearly-price').textContent = (data.yearly_net_price || 41.99).toFixed(2);
                    document.getElementById('be-fixed').textContent = (data.monthly_fixed_cost || 0).toFixed(2);
                    document.getElementById('be-variable').textContent = (data.variable_cost_per_user || 0.05).toFixed(2);
                    document.getElementById('be-total-monthly').textContent = (data.total_monthly_cost || 0).toFixed(2);
                }} catch (e) {{ console.error('Break-even error:', e); }}
            }}

            async function loadSummary() {{
                try {{
                    const res = await fetch(`/api/v1/admin/summary?password=${{password}}`);
                    const data = await res.json();
                    document.getElementById('variable-costs').textContent = '$' + (data.total_cost || 0).toFixed(2);
                    document.getElementById('active-users').textContent = data.unique_users || 0;
                }} catch (e) {{ console.error('Summary error:', e); }}
            }}

            async function loadComprehensiveSummary() {{
                try {{
                    const res = await fetch(`/api/v1/admin/comprehensive?password=${{password}}`);
                    const data = await res.json();
                    document.getElementById('total-monthly-cost').textContent = '$' + (data.total_monthly_cost || 0).toFixed(2);
                    document.getElementById('fixed-costs').textContent = '$' + (data.monthly_fixed_cost || 0).toFixed(2);
                    document.getElementById('cost-per-user').textContent = '$' + (data.cost_per_active_user || 0).toFixed(2);
                    document.getElementById('gross-revenue').textContent = '$' + (data.total_gross_revenue || 0).toFixed(2);
                    document.getElementById('app-store-commission').textContent = '$' + (data.app_store_commission || 0).toFixed(2);
                    document.getElementById('net-revenue').textContent = '$' + (data.total_net_revenue || 0).toFixed(2);
                    const profit = data.monthly_profit || 0;
                    const profitEl = document.getElementById('monthly-profit');
                    profitEl.textContent = (profit >= 0 ? '+$' : '-$') + Math.abs(profit).toFixed(2);
                    profitEl.className = 'stat small ' + (profit >= 0 ? 'profit' : 'loss');
                    const industryAvg = 0.70;
                    const yourCost = data.cost_per_active_user || 0;
                    const badge = document.getElementById('cost-badge');
                    if (yourCost < industryAvg) {{
                        badge.textContent = 'Below Average';
                        badge.className = 'badge below';
                    }} else if (yourCost > industryAvg * 1.5) {{
                        badge.textContent = 'High Cost';
                        badge.className = 'badge high';
                    }} else {{
                        badge.textContent = 'Average';
                        badge.className = 'badge average';
                    }}
                }} catch (e) {{ console.error('Comprehensive summary error:', e); }}
            }}

            async function loadDailyData() {{
                try {{
                    const res = await fetch(`/api/v1/admin/daily?password=${{password}}`);
                    const daily = await res.json();
                    const html = daily.map(d => `
                        <tr>
                            <td>${{d.date}}</td>
                            <td>${{d.openai_vision_calls || 0}}</td>
                            <td>${{d.vbt_analysis_calls || 0}}</td>
                            <td>${{d.ai_coach_messages || 0}}</td>
                            <td>${{d.storage_uploads || 0}}</td>
                            <td class="cost">${{d.total_estimated_cost ? '$' + d.total_estimated_cost.toFixed(4) : '$0.0000'}}</td>
                        </tr>
                    `).join('') || '<tr><td colspan="6">No data yet</td></tr>';
                    document.getElementById('daily-table').innerHTML = html;
                }} catch (e) {{ console.error('Daily error:', e); }}
            }}

            async function loadUsersData() {{
                try {{
                    const res = await fetch(`/api/v1/admin/users?password=${{password}}`);
                    const users = await res.json();
                    const html = users.map(u => `
                        <tr>
                            <td title="${{u.user_id}}">${{u.user_id ? u.user_id.substring(0, 8) + '...' : 'Unknown'}}</td>
                            <td>${{u.first_activity ? u.first_activity.split('T')[0] : '-'}}</td>
                            <td>${{u.active_days || 0}}</td>
                            <td>${{u.total_events || 0}}</td>
                            <td class="cost">${{u.total_estimated_cost ? '$' + u.total_estimated_cost.toFixed(4) : '$0.0000'}}</td>
                        </tr>
                    `).join('') || '<tr><td colspan="5">No data yet</td></tr>';
                    document.getElementById('users-table').innerHTML = html;
                }} catch (e) {{ console.error('Users error:', e); }}
            }}

            async function loadEventsData() {{
                try {{
                    const res = await fetch(`/api/v1/admin/events?password=${{password}}`);
                    const events = await res.json();
                    const html = events.map(e => `
                        <tr>
                            <td>${{e.event_type}}</td>
                            <td>${{e.count || 0}}</td>
                            <td>${{e.avg_tokens ? e.avg_tokens.toFixed(0) : '-'}}</td>
                            <td class="cost">${{e.total_cost ? '$' + e.total_cost.toFixed(4) : '$0.0000'}}</td>
                        </tr>
                    `).join('') || '<tr><td colspan="4">No data yet</td></tr>';
                    document.getElementById('events-table').innerHTML = html;
                }} catch (e) {{ console.error('Events error:', e); }}
            }}

            async function loadFixedCosts() {{
                try {{
                    const res = await fetch(`/api/v1/admin/fixed-costs?password=${{password}}`);
                    const costs = await res.json();
                    const html = costs.map(c => `
                        <tr>
                            <td>${{c.name}}</td>
                            <td>${{c.category}}</td>
                            <td>${{c.amount ? '$' + parseFloat(c.amount).toFixed(2) : '$0.00'}}</td>
                            <td>${{c.recurrence_type}}</td>
                            <td class="cost">${{c.monthly_equivalent ? '$' + parseFloat(c.monthly_equivalent).toFixed(2) : '-'}}</td>
                            <td>
                                <button class="btn secondary" onclick="editFixedCost('${{c.id}}', '${{c.name}}', '${{c.category}}', ${{c.amount}}, '${{c.recurrence_type}}')" style="padding: 4px 10px; font-size: 12px; margin-right: 5px;">Edit</button>
                                <button class="btn danger" onclick="deleteFixedCost('${{c.id}}')" style="padding: 4px 10px; font-size: 12px;">Delete</button>
                            </td>
                        </tr>
                    `).join('') || '<tr><td colspan="6">No fixed costs added yet.</td></tr>';
                    document.getElementById('fixed-table').innerHTML = html;
                }} catch (e) {{ console.error('Fixed costs error:', e); }}
            }}

            async function loadRevenue() {{
                try {{
                    document.getElementById('monthly-summary').style.display = 'none';
                    const res = await fetch(`/api/v1/admin/revenue?password=${{password}}`);
                    const revenue = await res.json();
                    const html = revenue.map(r => `
                        <tr>
                            <td>${{r.period_start}}</td>
                            <td>${{r.platform}}</td>
                            <td>${{r.revenue_type}}</td>
                            <td>${{r.gross_revenue ? '$' + parseFloat(r.gross_revenue).toFixed(2) : '$0.00'}}</td>
                            <td class="loss">${{r.app_store_commission ? '$' + parseFloat(r.app_store_commission).toFixed(2) : '$0.00'}}</td>
                            <td class="cost">${{r.net_revenue ? '$' + parseFloat(r.net_revenue).toFixed(2) : '$0.00'}}</td>
                            <td>
                                <button class="btn secondary" onclick="editRevenue('${{r.id}}', '${{r.platform}}', '${{r.revenue_type}}', ${{r.gross_revenue}}, '${{r.period_start}}')" style="padding: 4px 10px; font-size: 12px; margin-right: 5px;">Edit</button>
                                <button class="btn danger" onclick="deleteRevenue('${{r.id}}')" style="padding: 4px 10px; font-size: 12px;">Delete</button>
                            </td>
                        </tr>
                    `).join('') || '<tr><td colspan="7">No revenue recorded yet</td></tr>';
                    document.getElementById('revenue-table').innerHTML = html;
                }} catch (e) {{ console.error('Revenue error:', e); }}
            }}

            async function addFixedCost() {{
                const name = document.getElementById('fc-name').value;
                const category = document.getElementById('fc-category').value;
                const amount = parseFloat(document.getElementById('fc-amount').value);
                const recurrence = document.getElementById('fc-recurrence').value;
                if (!name || !amount) {{ alert('Please fill in name and amount'); return; }}
                try {{
                    const res = await fetch(`/api/v1/admin/fixed-costs?password=${{password}}`, {{
                        method: 'POST',
                        headers: {{ 'Content-Type': 'application/json' }},
                        body: JSON.stringify({{ name, category, amount, recurrence_type: recurrence }})
                    }});
                    if (res.ok) {{
                        document.getElementById('fc-name').value = '';
                        document.getElementById('fc-amount').value = '';
                        loadFixedCosts();
                        loadComprehensiveSummary();
                    }} else {{ alert('Failed to add cost'); }}
                }} catch (e) {{ alert('Error: ' + e.message); }}
            }}

            async function deleteFixedCost(id) {{
                if (!confirm('Delete this cost?')) return;
                try {{
                    const res = await fetch(`/api/v1/admin/fixed-costs/${{id}}?password=${{password}}`, {{ method: 'DELETE' }});
                    if (res.ok) {{ loadFixedCosts(); loadComprehensiveSummary(); }}
                }} catch (e) {{ alert('Error: ' + e.message); }}
            }}

            async function editFixedCost(id, name, category, amount, recurrence) {{
                const newName = prompt('Name:', name);
                if (newName === null) return;
                const newAmount = prompt('Amount ($):', amount);
                if (newAmount === null) return;
                const newCategory = prompt('Category:', category);
                if (newCategory === null) return;
                const newRecurrence = prompt('Recurrence (monthly/yearly/one_time):', recurrence);
                if (newRecurrence === null) return;
                try {{
                    const res = await fetch(`/api/v1/admin/fixed-costs/${{id}}?password=${{password}}`, {{
                        method: 'PUT',
                        headers: {{ 'Content-Type': 'application/json' }},
                        body: JSON.stringify({{ name: newName, category: newCategory, amount: parseFloat(newAmount), recurrence_type: newRecurrence }})
                    }});
                    if (res.ok) {{ loadFixedCosts(); loadComprehensiveSummary(); loadBreakEven(); }}
                    else {{ alert('Failed to update cost'); }}
                }} catch (e) {{ alert('Error: ' + e.message); }}
            }}

            async function addRevenue() {{
                const platform = document.getElementById('rev-platform').value;
                const type = document.getElementById('rev-type').value;
                const amount = parseFloat(document.getElementById('rev-amount').value);
                const date = document.getElementById('rev-date').value;
                if (!amount || !date) {{ alert('Please fill in amount and date'); return; }}
                try {{
                    const res = await fetch(`/api/v1/admin/revenue?password=${{password}}`, {{
                        method: 'POST',
                        headers: {{ 'Content-Type': 'application/json' }},
                        body: JSON.stringify({{ platform, revenue_type: type, gross_revenue: amount, period_start: date, period_end: date }})
                    }});
                    if (res.ok) {{
                        document.getElementById('rev-amount').value = '';
                        loadRevenue();
                        loadComprehensiveSummary();
                    }} else {{ alert('Failed to add revenue'); }}
                }} catch (e) {{ alert('Error: ' + e.message); }}
            }}

            async function deleteRevenue(id) {{
                if (!confirm('Delete this revenue entry?')) return;
                try {{
                    const res = await fetch(`/api/v1/admin/revenue/${{id}}?password=${{password}}`, {{ method: 'DELETE' }});
                    if (res.ok) {{ loadRevenue(); loadComprehensiveSummary(); }}
                }} catch (e) {{ alert('Error: ' + e.message); }}
            }}

            async function editRevenue(id, platform, type, amount, date) {{
                const newPlatform = prompt('Platform (ios/android/web):', platform);
                if (newPlatform === null) return;
                const newType = prompt('Type (subscription/one_time_purchase/ad_revenue):', type);
                if (newType === null) return;
                const newAmount = prompt('Gross Revenue ($):', amount);
                if (newAmount === null) return;
                const newDate = prompt('Date (YYYY-MM-DD):', date);
                if (newDate === null) return;
                try {{
                    const res = await fetch(`/api/v1/admin/revenue/${{id}}?password=${{password}}`, {{
                        method: 'PUT',
                        headers: {{ 'Content-Type': 'application/json' }},
                        body: JSON.stringify({{ platform: newPlatform, revenue_type: newType, gross_revenue: parseFloat(newAmount), period_start: newDate, period_end: newDate }})
                    }});
                    if (res.ok) {{ loadRevenue(); loadComprehensiveSummary(); }}
                    else {{ alert('Failed to update revenue'); }}
                }} catch (e) {{ alert('Error: ' + e.message); }}
            }}

            async function filterRevenueByMonth() {{
                const monthInput = document.getElementById('rev-month-filter').value;
                if (!monthInput) {{ alert('Please select a month'); return; }}
                try {{
                    const res = await fetch(`/api/v1/admin/revenue?password=${{password}}&month=${{monthInput}}`);
                    const allRevenue = await res.json();
                    const filtered = allRevenue.filter(r => r.period_start && r.period_start.startsWith(monthInput));
                    let totalGross = 0, totalCommission = 0, totalNet = 0;
                    filtered.forEach(r => {{
                        totalGross += parseFloat(r.gross_revenue || 0);
                        totalCommission += parseFloat(r.app_store_commission || 0);
                        totalNet += parseFloat(r.net_revenue || 0);
                    }});
                    const summaryDiv = document.getElementById('monthly-summary');
                    summaryDiv.style.display = 'block';
                    const [year, month] = monthInput.split('-');
                    const monthNames = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
                    document.getElementById('summary-month').textContent = `${{monthNames[parseInt(month) - 1]}} ${{year}}`;
                    document.getElementById('summary-gross').textContent = '$' + totalGross.toFixed(2);
                    document.getElementById('summary-commission').textContent = '-$' + totalCommission.toFixed(2);
                    document.getElementById('summary-net').textContent = '$' + totalNet.toFixed(2);
                    document.getElementById('summary-count').textContent = filtered.length;
                    const html = filtered.map(r => `
                        <tr>
                            <td>${{r.period_start}}</td>
                            <td>${{r.platform}}</td>
                            <td>${{r.revenue_type}}</td>
                            <td>${{r.gross_revenue ? '$' + parseFloat(r.gross_revenue).toFixed(2) : '$0.00'}}</td>
                            <td class="loss">${{r.app_store_commission ? '$' + parseFloat(r.app_store_commission).toFixed(2) : '$0.00'}}</td>
                            <td class="cost">${{r.net_revenue ? '$' + parseFloat(r.net_revenue).toFixed(2) : '$0.00'}}</td>
                            <td>
                                <button class="btn secondary" onclick="editRevenue('${{r.id}}', '${{r.platform}}', '${{r.revenue_type}}', ${{r.gross_revenue}}, '${{r.period_start}}')" style="padding: 4px 10px; font-size: 12px; margin-right: 5px;">Edit</button>
                                <button class="btn danger" onclick="deleteRevenue('${{r.id}}')" style="padding: 4px 10px; font-size: 12px;">Delete</button>
                            </td>
                        </tr>
                    `).join('') || `<tr><td colspan="7">No revenue for ${{monthNames[parseInt(month) - 1]}} ${{year}}</td></tr>`;
                    document.getElementById('revenue-table').innerHTML = html;
                }} catch (e) {{ console.error('Filter error:', e); alert('Error filtering: ' + e.message); }}
            }}

            // ========== SALARY FUNCTIONS ==========
            async function loadSalaries() {{
                try {{
                    const res = await fetch(`/api/v1/admin/employees?password=${{password}}`);
                    const data = await res.json();

                    // Update summary section
                    document.getElementById('sal-gross').textContent = '$' + (data.gross_revenue || 0).toFixed(2);
                    document.getElementById('sal-costs').textContent = '-$' + (data.operating_costs || 0).toFixed(2);
                    document.getElementById('sal-reserve').textContent = '-$' + (data.reserve_amount || 0).toFixed(2);
                    document.getElementById('sal-net').textContent = '$' + (data.net_available || 0).toFixed(2);
                    document.getElementById('sal-total').textContent = '$' + (data.total_salaries || 0).toFixed(2);

                    const employees = data.employees || [];
                    const html = employees.map(e => {{
                        const targetPercent = e.base_salary > 0 ? ((e.calculated_salary / e.base_salary) * 100).toFixed(0) : 0;
                        const percentColor = targetPercent >= 100 ? '#4ade80' : targetPercent >= 50 ? '#fbbf24' : '#ef4444';
                        return `
                            <tr>
                                <td>${{e.name}}</td>
                                <td>${{e.role || '-'}}</td>
                                <td>${{e.base_salary ? '$' + parseFloat(e.base_salary).toFixed(2) : '$0.00'}}</td>
                                <td>${{e.revenue_share_percent ? parseFloat(e.revenue_share_percent).toFixed(1) + '%' : '0%'}}</td>
                                <td class="cost">${{e.calculated_salary ? '$' + parseFloat(e.calculated_salary).toFixed(2) : '$0.00'}}</td>
                                <td style="color: ${{percentColor}}">${{targetPercent}}%</td>
                                <td>
                                    <button class="btn secondary" onclick="editEmployee('${{e.id}}', '${{e.name}}', '${{e.role || ''}}', ${{e.base_salary || 0}}, ${{e.revenue_share_percent || 0}})" style="padding: 4px 10px; font-size: 12px; margin-right: 5px;">Edit</button>
                                    <button class="btn danger" onclick="deleteEmployee('${{e.id}}')" style="padding: 4px 10px; font-size: 12px;">Delete</button>
                                </td>
                            </tr>
                        `;
                    }}).join('') || '<tr><td colspan="7">No employees added yet. Add employees above.</td></tr>';
                    document.getElementById('salaries-table').innerHTML = html;
                }} catch (e) {{ console.error('Salaries error:', e); }}
            }}

            async function addEmployee() {{
                const name = document.getElementById('emp-name').value;
                const role = document.getElementById('emp-role').value;
                const baseSalary = parseFloat(document.getElementById('emp-base-salary').value) || 0;
                const share = parseFloat(document.getElementById('emp-share').value) || 0;
                if (!name) {{ alert('Please enter employee name'); return; }}
                try {{
                    const res = await fetch(`/api/v1/admin/employees?password=${{password}}`, {{
                        method: 'POST',
                        headers: {{ 'Content-Type': 'application/json' }},
                        body: JSON.stringify({{ name, role, base_salary: baseSalary, revenue_share_percent: share }})
                    }});
                    if (res.ok) {{
                        document.getElementById('emp-name').value = '';
                        document.getElementById('emp-role').value = '';
                        document.getElementById('emp-base-salary').value = '';
                        document.getElementById('emp-share').value = '';
                        loadSalaries();
                    }} else {{ alert('Failed to add employee'); }}
                }} catch (e) {{ alert('Error: ' + e.message); }}
            }}

            async function editEmployee(id, name, role, baseSalary, share) {{
                const newName = prompt('Name:', name);
                if (newName === null) return;
                const newRole = prompt('Role:', role);
                if (newRole === null) return;
                const newBaseSalary = prompt('Base Salary (target) $:', baseSalary);
                if (newBaseSalary === null) return;
                const newShare = prompt('Revenue Share %:', share);
                if (newShare === null) return;
                try {{
                    const res = await fetch(`/api/v1/admin/employees/${{id}}?password=${{password}}`, {{
                        method: 'PUT',
                        headers: {{ 'Content-Type': 'application/json' }},
                        body: JSON.stringify({{ name: newName, role: newRole, base_salary: parseFloat(newBaseSalary), revenue_share_percent: parseFloat(newShare) }})
                    }});
                    if (res.ok) {{ loadSalaries(); }}
                    else {{ alert('Failed to update employee'); }}
                }} catch (e) {{ alert('Error: ' + e.message); }}
            }}

            async function deleteEmployee(id) {{
                if (!confirm('Delete this employee?')) return;
                try {{
                    const res = await fetch(`/api/v1/admin/employees/${{id}}?password=${{password}}`, {{ method: 'DELETE' }});
                    if (res.ok) {{ loadSalaries(); }}
                }} catch (e) {{ alert('Error: ' + e.message); }}
            }}

            function showTab(tab) {{
                document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
                document.querySelectorAll('[id$="-tab"]').forEach(t => t.style.display = 'none');
                event.target.classList.add('active');
                document.getElementById(tab + '-tab').style.display = 'block';
            }}

            document.getElementById('rev-date').valueAsDate = new Date();
            const now = new Date();
            document.getElementById('rev-month-filter').value = now.toISOString().slice(0, 7);
            loadAllData();
        </script>
    </body>
    </html>
    """


# ============================================================
# ADMIN AUTHENTICATION ENDPOINTS
# ============================================================

@router.post("/api/v1/admin/auth/login")
async def admin_login(request_data: dict):
    """
    Login endpoint for admin dashboard.
    Verifies user_id + email combination against authorized admins.
    """
    email = request_data.get("email", "").lower().strip()
    user_id = request_data.get("user_id", "").strip()

    if not email or not user_id:
        return {"success": False, "error": "Email and User ID are required"}

    # Check if user is authorized admin
    if user_id not in AUTHORIZED_ADMINS:
        print(f"Admin login failed: Unknown user_id {user_id}")
        return {"success": False, "error": "Unauthorized access"}

    # Verify email matches
    expected_email = AUTHORIZED_ADMINS[user_id].lower()
    if email != expected_email:
        print(f"Admin login failed: Email mismatch for {user_id}")
        return {"success": False, "error": "Unauthorized access"}

    # Return admin password as session token for API calls
    print(f"Admin login successful: {email}")

    return {
        "success": True,
        "session_token": ADMIN_PASSWORD,
        "email": email
    }


@router.post("/api/v1/admin/auth/logout")
async def admin_logout(request_data: dict):
    """Logout from admin dashboard"""
    session_token = request_data.get("session_token")
    if session_token and session_token in admin_sessions:
        del admin_sessions[session_token]
    return {"success": True}


# ============================================================
# ADMIN DASHBOARD ENDPOINT
# ============================================================

@router.get("/admin")
async def admin_dashboard(password: Optional[str] = None, session: Optional[str] = None):
    """
    Admin Dashboard - Comprehensive cost tracking and analytics
    Access methods:
    1. Session-based: /admin?session=SESSION_TOKEN (preferred)
    2. Legacy password: /admin?password=YOUR_ADMIN_PASSWORD
    """
    # Check session-based auth first
    if session:
        session_data = verify_session(session)
        if session_data:
            print(f"Admin dashboard accessed via session: {session_data['email']}")
            # Use session token as "password" for API calls
            return HTMLResponse(content=get_admin_dashboard_html(session))

    # Legacy password auth
    if password and verify_admin(password):
        return HTMLResponse(content=get_admin_dashboard_html(password))

    # Show login page
    return HTMLResponse(content=ADMIN_LOGIN_HTML, status_code=200)


# ============================================================
# ADMIN API ENDPOINTS
# ============================================================

@router.get("/api/v1/admin/summary")
async def admin_get_summary(password: str):
    """Get monthly cost summary"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        client = get_supabase_client()
        result = client.table('monthly_cost_summary').select('*').limit(1).execute()

        if result.data and len(result.data) > 0:
            data = result.data[0]
            return {
                "total_cost": float(data.get('total_estimated_cost') or 0),
                "unique_users": data.get('unique_users', 0),
                "total_events": data.get('total_events', 0),
                "avg_cost_per_user": float(data.get('avg_cost_per_user') or 0)
            }

        return {"total_cost": 0, "unique_users": 0, "total_events": 0, "avg_cost_per_user": 0}

    except Exception as e:
        print(f"Admin summary error: {str(e)}")
        return {"error": str(e), "total_cost": 0, "unique_users": 0, "total_events": 0}


@router.get("/api/v1/admin/daily")
async def admin_get_daily_costs(password: str, days: int = 30):
    """Get daily cost breakdown"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        client = get_supabase_client()
        result = client.table('daily_cost_summary')\
            .select('*')\
            .order('date', desc=True)\
            .limit(days)\
            .execute()

        return result.data or []

    except Exception as e:
        print(f"Admin daily error: {str(e)}")
        return []


@router.get("/api/v1/admin/users")
async def admin_get_user_costs(password: str, limit: int = 50):
    """Get per-user cost breakdown with profile data"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        client = get_supabase_client()

        # Get cost data
        cost_result = client.table('user_cost_summary')\
            .select('*')\
            .order('total_estimated_cost', desc=True)\
            .limit(limit)\
            .execute()

        cost_data = cost_result.data or []
        user_ids = [u['user_id'] for u in cost_data]

        # Get profile data
        profiles = {}
        if user_ids:
            profile_result = client.table('user_profiles')\
                .select('id, name, email, weight, height, age, created_at, updated_at')\
                .in_('id', user_ids)\
                .execute()
            profiles = {p['id']: p for p in (profile_result.data or [])}

        # Combine data into frontend expected format
        users = []
        total_cost = 0

        for u in cost_data:
            uid = u['user_id']
            profile = profiles.get(uid, {})
            cost = u.get('total_estimated_cost', 0)
            total_cost += cost

            users.append({
                "user_id": uid,
                "email": profile.get('email') or f"user-{uid[:8]}@app",
                "display_name": profile.get('name', 'User'),
                "created_at": profile.get('created_at') or u.get('first_activity'),
                "last_active": u.get('last_activity'),
                "subscription": {
                    "status": "active",  # TODO: Add real subscription data
                    "platform": None,
                    "plan_type": None,
                    "auto_renew": False
                },
                "costs": {
                    "ai_coach_messages": u.get('ai_coach_messages', 0),
                    "ai_coach_cost": u.get('total_estimated_cost', 0) * 0.7,  # Estimate
                    "photo_analysis_count": u.get('openai_vision_calls', 0),
                    "photo_analysis_cost": u.get('total_estimated_cost', 0) * 0.2,
                    "vbt_analysis_count": u.get('vbt_analysis_calls', 0),
                    "vbt_analysis_cost": u.get('total_estimated_cost', 0) * 0.1,
                    "storage_uploads": 0,
                    "storage_cost": 0,
                    "total_cost": cost
                },
                "is_profitable": False,  # No revenue tracking yet
                "lifetime_value": 0
            })

        return {
            "users": users,
            "total_count": len(users),
            "total_cost": total_cost,
            "total_revenue": 0,
            "profitable_users": 0,
            "unprofitable_users": len(users)
        }

    except Exception as e:
        print(f"Admin users error: {str(e)}")
        import traceback
        traceback.print_exc()
        return {"users": [], "total_count": 0, "total_cost": 0, "total_revenue": 0, "profitable_users": 0, "unprofitable_users": 0}


@router.get("/api/v1/admin/events")
async def admin_get_event_breakdown(password: str):
    """Get event type breakdown for current month"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from datetime import datetime
        client = get_supabase_client()

        now = datetime.now()
        month_start = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)

        try:
            result = client.rpc('get_event_breakdown', {'start_date': month_start.isoformat()}).execute()
            if result.data:
                return result.data
        except:
            pass

        result = client.table('usage_logs')\
            .select('event_type, total_tokens, estimated_cost')\
            .gte('created_at', month_start.isoformat())\
            .execute()

        event_stats = {}
        for row in result.data or []:
            et = row['event_type']
            if et not in event_stats:
                event_stats[et] = {'count': 0, 'total_tokens': 0, 'total_cost': 0}
            event_stats[et]['count'] += 1
            event_stats[et]['total_tokens'] += row.get('total_tokens') or 0
            event_stats[et]['total_cost'] += float(row.get('estimated_cost') or 0)

        return [
            {
                'event_type': et,
                'count': stats['count'],
                'avg_tokens': stats['total_tokens'] / stats['count'] if stats['count'] > 0 else 0,
                'total_cost': stats['total_cost']
            }
            for et, stats in event_stats.items()
        ]

    except Exception as e:
        print(f"Admin events error: {str(e)}")
        return []


@router.get("/api/v1/admin/service-costs")
async def admin_get_service_costs(password: str):
    """Get cost breakdown by service type for current month"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from datetime import datetime
        client = get_supabase_client()

        now = datetime.now()
        month_start = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)

        # Fetch all usage logs for this month
        result = client.table('usage_logs')\
            .select('event_type, estimated_cost')\
            .gte('created_at', month_start.isoformat())\
            .execute()

        # Aggregate by service type
        service_costs = {
            'ai_coach': {'cost': 0, 'count': 0},
            'photo_analysis': {'cost': 0, 'count': 0},
            'vbt': {'cost': 0, 'count': 0},
            'storage': {'cost': 0, 'count': 0}
        }

        for row in result.data or []:
            event_type = row.get('event_type', '')
            cost = float(row.get('estimated_cost') or 0)

            if 'ai_coach' in event_type.lower() or 'coach' in event_type.lower():
                service_costs['ai_coach']['cost'] += cost
                service_costs['ai_coach']['count'] += 1
            elif 'vision' in event_type.lower() or 'photo' in event_type.lower() or 'nutrition' in event_type.lower() or 'meal' in event_type.lower():
                service_costs['photo_analysis']['cost'] += cost
                service_costs['photo_analysis']['count'] += 1
            elif 'vbt' in event_type.lower() or 'video' in event_type.lower() or 'form' in event_type.lower():
                service_costs['vbt']['cost'] += cost
                service_costs['vbt']['count'] += 1
            elif 'storage' in event_type.lower() or 'upload' in event_type.lower():
                service_costs['storage']['cost'] += cost
                service_costs['storage']['count'] += 1
            else:
                # Default to photo analysis for unclassified OpenAI calls
                service_costs['photo_analysis']['cost'] += cost
                service_costs['photo_analysis']['count'] += 1

        return service_costs

    except Exception as e:
        print(f"Admin service costs error: {str(e)}")
        return {
            'ai_coach': {'cost': 0, 'count': 0},
            'photo_analysis': {'cost': 0, 'count': 0},
            'vbt': {'cost': 0, 'count': 0},
            'storage': {'cost': 0, 'count': 0}
        }


@router.get("/api/v1/admin/comprehensive")
async def admin_get_comprehensive_summary(password: str):
    """Get comprehensive cost summary including fixed costs and revenue"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from datetime import datetime
        client = get_supabase_client()

        try:
            result = client.table('comprehensive_cost_summary').select('*').limit(1).execute()
            if result.data and len(result.data) > 0:
                data = result.data[0]
                return {
                    "total_variable_cost": float(data.get('total_variable_cost') or 0),
                    "monthly_fixed_cost": float(data.get('monthly_fixed_cost') or 0),
                    "total_monthly_cost": float(data.get('total_monthly_cost') or 0),
                    "active_users": data.get('active_users', 0),
                    "total_events": data.get('total_events', 0),
                    "cost_per_active_user": float(data.get('cost_per_active_user') or 0),
                    "total_gross_revenue": float(data.get('total_gross_revenue') or 0),
                    "app_store_commission": float(data.get('app_store_commission') or 0),
                    "total_net_revenue": float(data.get('total_net_revenue') or 0),
                    "monthly_profit": float(data.get('monthly_profit') or 0),
                    "revenue_per_active_user": float(data.get('revenue_per_active_user') or 0)
                }
        except Exception as view_error:
            print(f"comprehensive_cost_summary view not found, calculating manually: {view_error}")

        now = datetime.now()
        month_start = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)

        usage_result = client.table('usage_logs')\
            .select('user_id, estimated_cost')\
            .gte('created_at', month_start.isoformat())\
            .execute()

        total_variable_cost = sum(float(r.get('estimated_cost') or 0) for r in (usage_result.data or []))
        active_users = len(set(r.get('user_id') for r in (usage_result.data or []) if r.get('user_id')))
        total_events = len(usage_result.data or [])

        monthly_fixed_cost = 0
        try:
            fixed_result = client.table('fixed_costs')\
                .select('amount, recurrence_type')\
                .eq('is_recurring', True)\
                .execute()

            for fc in (fixed_result.data or []):
                amount = float(fc.get('amount') or 0)
                if fc.get('recurrence_type') == 'monthly':
                    monthly_fixed_cost += amount
                elif fc.get('recurrence_type') == 'yearly':
                    monthly_fixed_cost += amount / 12
        except:
            pass

        total_gross_revenue = 0
        app_store_commission = 0
        total_net_revenue = 0
        try:
            revenue_result = client.table('app_revenue')\
                .select('gross_revenue, app_store_commission, net_revenue')\
                .gte('period_start', month_start.strftime('%Y-%m-%d'))\
                .execute()

            for rev in (revenue_result.data or []):
                total_gross_revenue += float(rev.get('gross_revenue') or 0)
                app_store_commission += float(rev.get('app_store_commission') or 0)
                total_net_revenue += float(rev.get('net_revenue') or 0)
        except:
            pass

        total_monthly_cost = total_variable_cost + monthly_fixed_cost
        cost_per_user = total_monthly_cost / active_users if active_users > 0 else 0
        monthly_profit = total_net_revenue - total_monthly_cost

        return {
            "total_variable_cost": total_variable_cost,
            "monthly_fixed_cost": monthly_fixed_cost,
            "total_monthly_cost": total_monthly_cost,
            "active_users": active_users,
            "total_events": total_events,
            "cost_per_active_user": cost_per_user,
            "total_gross_revenue": total_gross_revenue,
            "app_store_commission": app_store_commission,
            "total_net_revenue": total_net_revenue,
            "monthly_profit": monthly_profit,
            "revenue_per_active_user": total_net_revenue / active_users if active_users > 0 else 0
        }

    except Exception as e:
        print(f"Admin comprehensive error: {str(e)}")
        return {
            "total_variable_cost": 0, "monthly_fixed_cost": 0, "total_monthly_cost": 0,
            "active_users": 0, "cost_per_active_user": 0, "total_gross_revenue": 0,
            "app_store_commission": 0, "total_net_revenue": 0, "monthly_profit": 0
        }


@router.get("/api/v1/admin/fixed-costs")
async def admin_get_fixed_costs(password: str):
    """Get all fixed costs"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        client = get_supabase_client()
        result = client.table('fixed_costs')\
            .select('*')\
            .order('created_at', desc=True)\
            .execute()

        costs = []
        for fc in (result.data or []):
            amount = float(fc.get('amount') or 0)
            recurrence = fc.get('recurrence_type', 'monthly')

            if recurrence == 'monthly':
                monthly_eq = amount
            elif recurrence == 'yearly':
                monthly_eq = amount / 12
            else:
                monthly_eq = None

            costs.append({**fc, 'monthly_equivalent': monthly_eq})

        return costs

    except Exception as e:
        print(f"Admin fixed costs GET error: {str(e)}")
        return []


@router.post("/api/v1/admin/fixed-costs")
async def admin_add_fixed_cost(password: str, request_data: dict):
    """Add a new fixed cost"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        client = get_supabase_client()

        data = {
            "name": request_data.get("name"),
            "category": request_data.get("category", "other"),
            "amount": float(request_data.get("amount", 0)),
            "recurrence_type": request_data.get("recurrence_type", "monthly"),
            "is_recurring": request_data.get("recurrence_type") != "one_time",
            "description": request_data.get("description")
        }

        result = client.table('fixed_costs').insert(data).execute()

        if result.data:
            print(f"Added fixed cost: {data['name']} - ${data['amount']}")
            return {"success": True, "id": result.data[0].get("id")}

        return {"success": False, "error": "Insert failed"}

    except Exception as e:
        print(f"Admin fixed costs POST error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@router.delete("/api/v1/admin/fixed-costs/{cost_id}")
async def admin_delete_fixed_cost(cost_id: str, password: str):
    """Delete a fixed cost"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        client = get_supabase_client()
        client.table('fixed_costs').delete().eq('id', cost_id).execute()
        print(f"Deleted fixed cost: {cost_id}")
        return {"success": True}

    except Exception as e:
        print(f"Admin fixed costs DELETE error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@router.put("/api/v1/admin/fixed-costs/{cost_id}")
async def admin_update_fixed_cost(cost_id: str, password: str, request_data: dict):
    """Update a fixed cost"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        client = get_supabase_client()

        data = {
            "name": request_data.get("name"),
            "category": request_data.get("category", "other"),
            "amount": float(request_data.get("amount", 0)),
            "recurrence_type": request_data.get("recurrence_type", "monthly"),
            "is_recurring": request_data.get("recurrence_type") != "one_time",
            "description": request_data.get("description")
        }

        client.table('fixed_costs').update(data).eq('id', cost_id).execute()
        print(f"Updated fixed cost: {cost_id}")
        return {"success": True}

    except Exception as e:
        print(f"Admin fixed costs PUT error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/api/v1/admin/revenue")
async def admin_get_revenue(password: str):
    """Get all revenue entries"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        client = get_supabase_client()
        result = client.table('app_revenue')\
            .select('*')\
            .order('period_start', desc=True)\
            .limit(100)\
            .execute()

        return result.data or []

    except Exception as e:
        print(f"Admin revenue GET error: {str(e)}")
        return []


@router.post("/api/v1/admin/revenue")
async def admin_add_revenue(password: str, request_data: dict):
    """Add a new revenue entry"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        client = get_supabase_client()

        platform = request_data.get("platform", "ios")
        revenue_type = request_data.get("revenue_type", "subscription")
        gross_revenue = float(request_data.get("gross_revenue", 0))

        if platform == "web":
            commission_rate = 0
        else:
            commission_rate = 0.15

        if revenue_type in ["subscription", "one_time_purchase"]:
            app_store_commission = gross_revenue * commission_rate
        else:
            app_store_commission = 0

        net_revenue = gross_revenue - app_store_commission

        data = {
            "platform": platform,
            "revenue_type": revenue_type,
            "gross_revenue": gross_revenue,
            "app_store_commission": app_store_commission,
            "net_revenue": net_revenue,
            "period_start": request_data.get("period_start"),
            "period_end": request_data.get("period_end"),
            "notes": request_data.get("notes")
        }

        result = client.table('app_revenue').insert(data).execute()

        if result.data:
            print(f"Added revenue: {platform} ${gross_revenue} -> ${net_revenue} net")
            return {"success": True, "id": result.data[0].get("id")}

        return {"success": False, "error": "Insert failed"}

    except Exception as e:
        print(f"Admin revenue POST error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@router.delete("/api/v1/admin/revenue/{revenue_id}")
async def admin_delete_revenue(revenue_id: str, password: str):
    """Delete a revenue entry"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        client = get_supabase_client()
        client.table('app_revenue').delete().eq('id', revenue_id).execute()
        print(f"Deleted revenue entry: {revenue_id}")
        return {"success": True}

    except Exception as e:
        print(f"Admin revenue DELETE error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@router.put("/api/v1/admin/revenue/{revenue_id}")
async def admin_update_revenue(revenue_id: str, password: str, request_data: dict):
    """Update a revenue entry"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        client = get_supabase_client()

        platform = request_data.get("platform", "ios")
        revenue_type = request_data.get("revenue_type", "subscription")
        gross_revenue = float(request_data.get("gross_revenue", 0))

        if platform == "web":
            commission_rate = 0
        else:
            commission_rate = 0.15

        if revenue_type in ["subscription", "one_time_purchase"]:
            app_store_commission = gross_revenue * commission_rate
        else:
            app_store_commission = 0

        net_revenue = gross_revenue - app_store_commission

        data = {
            "platform": platform,
            "revenue_type": revenue_type,
            "gross_revenue": gross_revenue,
            "app_store_commission": app_store_commission,
            "net_revenue": net_revenue,
            "period_start": request_data.get("period_start"),
            "period_end": request_data.get("period_end"),
            "notes": request_data.get("notes")
        }

        client.table('app_revenue').update(data).eq('id', revenue_id).execute()
        print(f"Updated revenue: {revenue_id}")
        return {"success": True}

    except Exception as e:
        print(f"Admin revenue PUT error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/api/v1/admin/break-even")
async def admin_get_break_even_analysis(password: str):
    """Calculate break-even analysis for subscription business"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from datetime import datetime
        client = get_supabase_client()

        # Default values (industry benchmarks for fitness apps)
        monthly_gross_price = 9.99
        yearly_gross_price = 59.99
        commission_rate = 0.15
        monthly_churn_rate = 0.12
        yearly_renewal_rate = 0.45
        monthly_to_yearly_ratio = 0.65
        avg_monthly_lifetime = 4.5
        avg_yearly_renewals = 1.8
        variable_cost_per_user = 0.05

        try:
            metrics_result = client.table('business_metrics').select('*').execute()
            for metric in (metrics_result.data or []):
                key = metric.get('metric_key')
                value = float(metric.get('metric_value', 0))
                if key == 'monthly_sub_price':
                    monthly_gross_price = value
                elif key == 'yearly_sub_price':
                    yearly_gross_price = value
                elif key == 'apple_commission_rate':
                    commission_rate = value
                elif key == 'monthly_churn_rate':
                    monthly_churn_rate = value
                elif key == 'yearly_renewal_rate':
                    yearly_renewal_rate = value
                elif key == 'monthly_to_yearly_ratio':
                    monthly_to_yearly_ratio = value
                elif key == 'avg_monthly_ltv_months':
                    avg_monthly_lifetime = value
                elif key == 'avg_yearly_renewals':
                    avg_yearly_renewals = value
                elif key == 'variable_cost_per_active_user':
                    variable_cost_per_user = value
        except:
            pass

        monthly_net_price = monthly_gross_price * (1 - commission_rate)
        yearly_net_price = yearly_gross_price * (1 - commission_rate)

        monthly_ltv = monthly_net_price * avg_monthly_lifetime
        yearly_ltv = yearly_net_price * avg_yearly_renewals

        yearly_ratio = 1 - monthly_to_yearly_ratio
        blended_ltv = (monthly_to_yearly_ratio * monthly_ltv) + (yearly_ratio * yearly_ltv)

        monthly_fixed_cost = 0
        try:
            fixed_result = client.table('fixed_costs')\
                .select('amount, recurrence_type')\
                .eq('is_recurring', True)\
                .execute()

            for fc in (fixed_result.data or []):
                amount = float(fc.get('amount') or 0)
                if fc.get('recurrence_type') == 'monthly':
                    monthly_fixed_cost += amount
                elif fc.get('recurrence_type') == 'yearly':
                    monthly_fixed_cost += amount / 12
        except:
            pass

        now = datetime.now()
        month_start = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        try:
            usage_result = client.table('usage_logs')\
                .select('estimated_cost')\
                .gte('created_at', month_start.isoformat())\
                .execute()
            total_variable_cost = sum(float(r.get('estimated_cost') or 0) for r in (usage_result.data or []))
        except:
            total_variable_cost = 0

        total_monthly_cost = monthly_fixed_cost + total_variable_cost

        monthly_ltv_net = monthly_ltv - (variable_cost_per_user * avg_monthly_lifetime)
        if monthly_ltv_net > 0:
            monthly_subs_needed = int(total_monthly_cost / (monthly_ltv_net / avg_monthly_lifetime)) + 1
        else:
            monthly_subs_needed = 9999

        yearly_ltv_net = yearly_ltv - (variable_cost_per_user * avg_yearly_renewals * 12)
        yearly_monthly_contribution = yearly_ltv_net / (avg_yearly_renewals * 12) if avg_yearly_renewals > 0 else 0
        if yearly_monthly_contribution > 0:
            yearly_subs_needed = int(total_monthly_cost / yearly_monthly_contribution) + 1
        else:
            yearly_subs_needed = 9999

        blended_ltv_net = blended_ltv - (variable_cost_per_user * 6)
        blended_monthly_contribution = blended_ltv_net / 6 if blended_ltv_net > 0 else 0
        if blended_monthly_contribution > 0:
            mixed_subs_needed = int(total_monthly_cost / blended_monthly_contribution) + 1
        else:
            mixed_subs_needed = 9999

        return {
            "monthly_gross_price": monthly_gross_price,
            "yearly_gross_price": yearly_gross_price,
            "monthly_net_price": monthly_net_price,
            "yearly_net_price": yearly_net_price,
            "commission_rate": commission_rate,
            "monthly_ltv": monthly_ltv,
            "yearly_ltv": yearly_ltv,
            "blended_ltv": blended_ltv,
            "monthly_churn_rate": monthly_churn_rate,
            "yearly_renewal_rate": yearly_renewal_rate,
            "avg_monthly_lifetime": avg_monthly_lifetime,
            "avg_yearly_renewals": avg_yearly_renewals,
            "monthly_to_yearly_ratio": monthly_to_yearly_ratio,
            "monthly_fixed_cost": monthly_fixed_cost,
            "variable_cost_per_user": variable_cost_per_user,
            "total_monthly_cost": total_monthly_cost,
            "monthly_subs_needed": monthly_subs_needed,
            "yearly_subs_needed": yearly_subs_needed,
            "mixed_subs_needed": mixed_subs_needed
        }

    except Exception as e:
        print(f"Admin break-even error: {str(e)}")
        return {
            "monthly_subs_needed": 0,
            "yearly_subs_needed": 0,
            "mixed_subs_needed": 0,
            "error": str(e)
        }


# ============================================================
# ADMIN - PARTNER MANAGEMENT ENDPOINTS
# ============================================================

@router.get("/api/v1/admin/partners")
async def admin_get_partners(password: str):
    """Get all partners with their stats"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        client = get_supabase_client()

        try:
            result = client.table('partner_statistics').select('*').execute()
            return result.data or []
        except:
            result = client.table('partners').select('*').order('created_at', desc=True).execute()
            return result.data or []

    except Exception as e:
        print(f"Admin partners error: {e}")
        return []


@router.post("/api/v1/admin/partners")
async def admin_create_partner(request: Request, password: str):
    """Create a new partner"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        import hashlib
        data = await request.json()
        client = get_supabase_client()

        password_plain = data.get('password', 'naya2024')
        password_hash = hashlib.sha256(password_plain.encode()).hexdigest()

        referral_code = data.get('referral_code', '').upper()
        if not referral_code:
            name = data.get('name', 'PARTNER')
            base_code = ''.join(c for c in name.upper() if c.isalnum())[:6]
            referral_code = base_code if base_code else 'PARTNER'

            existing = client.table('partners').select('referral_code').eq('referral_code', referral_code).execute()
            counter = 1
            while existing.data:
                referral_code = f"{base_code}{counter}"
                existing = client.table('partners').select('referral_code').eq('referral_code', referral_code).execute()
                counter += 1

        partner_data = {
            'name': data.get('name'),
            'email': data.get('email', '').lower(),
            'referral_code': referral_code,
            'partner_type': data.get('partner_type', 'affiliate'),
            'commission_percent': data.get('commission_percent', 15),
            'status': data.get('status', 'active'),
            'instagram_handle': data.get('instagram_handle'),
            'follower_count': data.get('follower_count'),
            'payout_method': data.get('payout_method'),
            'payout_details': {'password_hash': password_hash},
            'notes': data.get('notes')
        }

        result = client.table('partners').insert(partner_data).execute()

        return {
            "success": True,
            "partner": result.data[0] if result.data else None,
            "generated_password": password_plain,
            "referral_code": referral_code
        }

    except Exception as e:
        print(f"Admin create partner error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.put("/api/v1/admin/partners/{partner_id}")
async def admin_update_partner(partner_id: str, request: Request, password: str):
    """Update a partner"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        import hashlib
        data = await request.json()
        client = get_supabase_client()

        update_data = {}

        if 'name' in data:
            update_data['name'] = data['name']
        if 'email' in data:
            update_data['email'] = data['email'].lower()
        if 'referral_code' in data:
            update_data['referral_code'] = data['referral_code'].upper()
        if 'partner_type' in data:
            update_data['partner_type'] = data['partner_type']
        if 'commission_percent' in data:
            update_data['commission_percent'] = data['commission_percent']
        if 'status' in data:
            update_data['status'] = data['status']
        if 'payout_method' in data:
            update_data['payout_method'] = data['payout_method']
        if 'notes' in data:
            update_data['notes'] = data['notes']
        if 'instagram_handle' in data:
            update_data['instagram_handle'] = data['instagram_handle']
        if 'follower_count' in data:
            update_data['follower_count'] = data['follower_count']

        if 'new_password' in data and data['new_password']:
            password_hash = hashlib.sha256(data['new_password'].encode()).hexdigest()
            existing = client.table('partners').select('payout_details').eq('id', partner_id).execute()
            payout_details = existing.data[0].get('payout_details', {}) if existing.data else {}
            payout_details['password_hash'] = password_hash
            update_data['payout_details'] = payout_details

        result = client.table('partners').update(update_data).eq('id', partner_id).execute()

        return {"success": True, "partner": result.data[0] if result.data else None}

    except Exception as e:
        print(f"Admin update partner error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.delete("/api/v1/admin/partners/{partner_id}")
async def admin_delete_partner(partner_id: str, password: str):
    """Delete a partner (soft delete - sets status to terminated)"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        client = get_supabase_client()
        client.table('partners').update({'status': 'terminated'}).eq('id', partner_id).execute()
        return {"success": True}

    except Exception as e:
        print(f"Admin delete partner error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/api/v1/admin/partner-referrals")
async def admin_get_all_referrals(password: str, partner_id: Optional[str] = None):
    """Get all referrals, optionally filtered by partner"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        client = get_supabase_client()
        query = client.table('partner_referrals').select('*, partners(name, referral_code)')

        if partner_id:
            query = query.eq('partner_id', partner_id)

        result = query.order('created_at', desc=True).limit(100).execute()
        return result.data or []

    except Exception as e:
        print(f"Admin referrals error: {e}")
        return []


@router.post("/api/v1/admin/partner-payouts")
async def admin_create_payout(request: Request, password: str):
    """Create a payout for a partner"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from datetime import datetime
        data = await request.json()
        client = get_supabase_client()

        partner_id = data.get('partner_id')

        referrals_result = client.table('partner_referrals')\
            .select('*')\
            .eq('partner_id', partner_id)\
            .eq('commission_status', 'confirmed')\
            .is_('payout_id', 'null')\
            .execute()

        referrals = referrals_result.data or []

        if not referrals:
            return {"success": False, "error": "No confirmed referrals to pay"}

        total_amount = sum(float(r.get('commission_amount', 0)) for r in referrals)

        payout_data = {
            'partner_id': partner_id,
            'amount': total_amount,
            'referral_count': len(referrals),
            'period_start': data.get('period_start', referrals[-1].get('created_at', '')[:10]),
            'period_end': data.get('period_end', referrals[0].get('created_at', '')[:10]),
            'status': 'pending'
        }

        payout_result = client.table('partner_payouts').insert(payout_data).execute()
        payout_id = payout_result.data[0]['id']

        for r in referrals:
            client.table('partner_referrals').update({
                'payout_id': payout_id,
                'commission_status': 'paid',
                'paid_at': datetime.now().isoformat()
            }).eq('id', r['id']).execute()

        return {"success": True, "payout_id": payout_id, "amount": total_amount, "referral_count": len(referrals)}

    except Exception as e:
        print(f"Admin create payout error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.put("/api/v1/admin/partner-payouts/{payout_id}")
async def admin_update_payout(payout_id: str, request: Request, password: str):
    """Update payout status (mark as completed, etc.)"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from datetime import datetime
        data = await request.json()
        client = get_supabase_client()

        update_data = {'status': data.get('status', 'completed')}

        if data.get('status') == 'completed':
            update_data['completed_at'] = datetime.now().isoformat()

        if 'payout_reference' in data:
            update_data['payout_reference'] = data['payout_reference']

        client.table('partner_payouts').update(update_data).eq('id', payout_id).execute()

        return {"success": True}

    except Exception as e:
        print(f"Admin update payout error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ============================================================
# ADMIN - EMPLOYEE SALARY MANAGEMENT ENDPOINTS
# ============================================================

@router.get("/api/v1/admin/employees")
async def admin_get_employees_with_salaries(password: str):
    """
    Get all employees with calculated salaries based on current month's net revenue.

    Salary Calculation:
    1. Gross Revenue (from app_revenue for current month)
    2. - Operating Costs (fixed_costs + variable API costs from usage_logs)
    3. - 15% Reserve
    4. = Net Available
    5. Employee Salary = Net Available × Revenue Share %
    """
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from datetime import datetime
        client = get_supabase_client()

        now = datetime.now()
        month_start = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)

        # Get gross revenue for current month
        gross_revenue = 0
        try:
            revenue_result = client.table('app_revenue')\
                .select('gross_revenue')\
                .gte('period_start', month_start.strftime('%Y-%m-%d'))\
                .execute()

            for rev in (revenue_result.data or []):
                gross_revenue += float(rev.get('gross_revenue') or 0)
        except Exception as e:
            print(f"Revenue fetch error: {e}")

        # Get fixed costs (monthly equivalent)
        monthly_fixed_cost = 0
        try:
            fixed_result = client.table('fixed_costs')\
                .select('amount, recurrence_type')\
                .eq('is_recurring', True)\
                .execute()

            for fc in (fixed_result.data or []):
                amount = float(fc.get('amount') or 0)
                if fc.get('recurrence_type') == 'monthly':
                    monthly_fixed_cost += amount
                elif fc.get('recurrence_type') == 'yearly':
                    monthly_fixed_cost += amount / 12
        except Exception as e:
            print(f"Fixed costs fetch error: {e}")

        # Get variable costs (API usage) for current month
        variable_costs = 0
        try:
            usage_result = client.table('usage_logs')\
                .select('estimated_cost')\
                .gte('created_at', month_start.isoformat())\
                .execute()

            variable_costs = sum(float(r.get('estimated_cost') or 0) for r in (usage_result.data or []))
        except Exception as e:
            print(f"Variable costs fetch error: {e}")

        # Calculate totals
        operating_costs = monthly_fixed_cost + variable_costs
        reserve_percent = 15.0
        reserve_amount = gross_revenue * (reserve_percent / 100)
        net_available = gross_revenue - operating_costs - reserve_amount

        # Get all active employees
        employees_result = client.table('employees')\
            .select('*')\
            .eq('is_active', True)\
            .order('created_at', desc=False)\
            .execute()

        employees = []
        total_salaries = 0

        for emp in (employees_result.data or []):
            revenue_share = float(emp.get('revenue_share_percent') or 0)
            calculated_salary = max(0, net_available * (revenue_share / 100))
            total_salaries += calculated_salary

            employees.append({
                'id': emp.get('id'),
                'name': emp.get('name'),
                'role': emp.get('role'),
                'base_salary': float(emp.get('base_salary') or 0),
                'revenue_share_percent': revenue_share,
                'calculated_salary': calculated_salary,
                'is_active': emp.get('is_active'),
                'start_date': emp.get('start_date'),
                'notes': emp.get('notes')
            })

        return {
            'gross_revenue': gross_revenue,
            'operating_costs': operating_costs,
            'reserve_percent': reserve_percent,
            'reserve_amount': reserve_amount,
            'net_available': net_available,
            'total_salaries': total_salaries,
            'employees': employees,
            'month': month_start.strftime('%Y-%m')
        }

    except Exception as e:
        print(f"Admin employees GET error: {str(e)}")
        import traceback
        traceback.print_exc()
        return {
            'gross_revenue': 0,
            'operating_costs': 0,
            'reserve_percent': 15.0,
            'reserve_amount': 0,
            'net_available': 0,
            'total_salaries': 0,
            'employees': [],
            'error': str(e)
        }


@router.post("/api/v1/admin/employees")
async def admin_add_employee(password: str, request_data: dict):
    """Add a new employee"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        client = get_supabase_client()

        data = {
            "name": request_data.get("name"),
            "role": request_data.get("role"),
            "base_salary": float(request_data.get("base_salary", 0)),
            "revenue_share_percent": float(request_data.get("revenue_share_percent", 0)),
            "is_active": True,
            "notes": request_data.get("notes")
        }

        if not data["name"]:
            raise HTTPException(status_code=400, detail="Employee name is required")

        result = client.table('employees').insert(data).execute()

        if result.data:
            print(f"Added employee: {data['name']} - {data['revenue_share_percent']}% share")
            return {"success": True, "id": result.data[0].get("id"), "employee": result.data[0]}

        return {"success": False, "error": "Insert failed"}

    except HTTPException:
        raise
    except Exception as e:
        print(f"Admin employees POST error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@router.put("/api/v1/admin/employees/{employee_id}")
async def admin_update_employee(employee_id: str, password: str, request_data: dict):
    """Update an employee"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        client = get_supabase_client()

        data = {
            "name": request_data.get("name"),
            "role": request_data.get("role"),
            "base_salary": float(request_data.get("base_salary", 0)),
            "revenue_share_percent": float(request_data.get("revenue_share_percent", 0)),
        }

        if "is_active" in request_data:
            data["is_active"] = request_data["is_active"]
        if "notes" in request_data:
            data["notes"] = request_data["notes"]

        result = client.table('employees').update(data).eq('id', employee_id).execute()
        print(f"Updated employee: {employee_id}")
        return {"success": True, "employee": result.data[0] if result.data else None}

    except Exception as e:
        print(f"Admin employees PUT error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@router.delete("/api/v1/admin/employees/{employee_id}")
async def admin_delete_employee(employee_id: str, password: str):
    """Delete an employee (hard delete)"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        client = get_supabase_client()
        client.table('employees').delete().eq('id', employee_id).execute()
        print(f"Deleted employee: {employee_id}")
        return {"success": True}

    except Exception as e:
        print(f"Admin employees DELETE error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/api/v1/admin/employees/calculate-month")
async def admin_calculate_monthly_salaries(password: str, request_data: dict):
    """
    Calculate and optionally store monthly salary calculations for all employees.
    This creates a snapshot for historical tracking.
    """
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from datetime import datetime
        client = get_supabase_client()

        month_year = request_data.get('month_year')
        if not month_year:
            month_year = datetime.now().strftime('%Y-%m')

        save_to_history = request_data.get('save_to_history', False)

        # Get current salary data
        employees_data = await admin_get_employees_with_salaries(password)

        if save_to_history:
            # Save salary calculations to history table
            for emp in employees_data.get('employees', []):
                calc_data = {
                    'employee_id': emp['id'],
                    'month_year': month_year,
                    'gross_revenue': employees_data['gross_revenue'],
                    'total_operating_costs': employees_data['operating_costs'],
                    'reserve_percent': employees_data['reserve_percent'],
                    'reserve_amount': employees_data['reserve_amount'],
                    'net_available': employees_data['net_available'],
                    'revenue_share_percent': emp['revenue_share_percent'],
                    'calculated_salary': emp['calculated_salary'],
                    'base_salary': emp['base_salary']
                }

                # Upsert (update if exists, insert if not)
                try:
                    # Try to find existing record
                    existing = client.table('salary_calculations')\
                        .select('id')\
                        .eq('employee_id', emp['id'])\
                        .eq('month_year', month_year)\
                        .execute()

                    if existing.data:
                        client.table('salary_calculations')\
                            .update(calc_data)\
                            .eq('id', existing.data[0]['id'])\
                            .execute()
                    else:
                        client.table('salary_calculations').insert(calc_data).execute()
                except Exception as e:
                    print(f"Error saving salary calculation for {emp['name']}: {e}")

            print(f"Saved salary calculations for {month_year}")

        return {
            'success': True,
            'month_year': month_year,
            'saved_to_history': save_to_history,
            **employees_data
        }

    except Exception as e:
        print(f"Admin calculate salaries error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


# ============================================================
# ADMIN - REVOLUT PAYOUT ENDPOINTS
# ============================================================

@router.get("/api/v1/admin/pending-payouts")
async def admin_get_pending_payouts(password: str):
    """Get all partners with pending payouts (confirmed commissions)"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from datetime import datetime, timedelta
        client = get_supabase_client()

        # Get all active partners
        partners_result = client.table('partners').select('*').eq('status', 'active').execute()
        partners = partners_result.data or []

        pending_payouts = []
        min_payout = 50.0  # Minimum €50 for payout

        for partner in partners:
            partner_id = partner['id']

            # Get confirmed referrals that haven't been paid
            referrals_result = client.table('partner_referrals')\
                .select('*')\
                .eq('partner_id', partner_id)\
                .eq('commission_status', 'confirmed')\
                .is_('payout_id', 'null')\
                .execute()

            referrals = referrals_result.data or []

            if not referrals:
                continue

            total_amount = sum(float(r.get('commission_amount', 0)) for r in referrals)

            # Get payout details (IBAN or Revolut email)
            payout_details = partner.get('payout_details', {}) or {}
            revolut_email = payout_details.get('revolut_email', '')
            iban = payout_details.get('iban', '')
            counterparty_id = payout_details.get('revolut_counterparty_id', '')

            has_payout_info = bool(revolut_email) or bool(iban) or bool(counterparty_id)

            pending_payouts.append({
                'partner_id': partner_id,
                'partner_name': partner.get('name', ''),
                'referral_code': partner.get('referral_code', ''),
                'email': partner.get('email', ''),
                'revolut_email': revolut_email,
                'iban': iban,
                'counterparty_id': counterparty_id,
                'referral_count': len(referrals),
                'total_amount': total_amount,
                'eligible': total_amount >= min_payout and has_payout_info,
                'missing_payout_info': not has_payout_info
            })

        # Sort by amount descending
        pending_payouts.sort(key=lambda x: x['total_amount'], reverse=True)

        return {
            'pending_payouts': pending_payouts,
            'total_pending': sum(p['total_amount'] for p in pending_payouts),
            'min_payout': min_payout
        }

    except Exception as e:
        print(f"Admin pending payouts error: {e}")
        import traceback
        traceback.print_exc()
        return {'pending_payouts': [], 'total_pending': 0, 'error': str(e)}


@router.post("/api/v1/admin/create-revolut-counterparty")
async def admin_create_revolut_counterparty(request: Request, password: str):
    """Create Revolut counterparty for a partner (required before payout)"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from prometheus_backend.revolut_service import get_revolut_service

        data = await request.json()
        partner_id = data.get('partner_id')

        if not partner_id:
            return {"success": False, "error": "Partner ID required"}

        client = get_supabase_client()

        # Get partner info
        partner_result = client.table('partners').select('*').eq('id', partner_id).execute()
        if not partner_result.data:
            return {"success": False, "error": "Partner not found"}

        partner = partner_result.data[0]
        payout_details = partner.get('payout_details', {}) or {}

        revolut_email = payout_details.get('revolut_email', '')
        iban = payout_details.get('iban', '')
        bic = payout_details.get('bic', '')
        bank_country = payout_details.get('bank_country', 'DE')

        if not revolut_email and not iban:
            return {"success": False, "error": "Partner has no Revolut email or IBAN configured"}

        # Create counterparty in Revolut
        revolut_service = get_revolut_service()

        if not revolut_service.configured:
            return {"success": False, "error": "Revolut not configured. Set REVOLUT_ACCESS_TOKEN."}

        counterparty_result = await revolut_service.create_counterparty(
            name=partner.get('name', 'Partner'),
            email=revolut_email if revolut_email else None,
            iban=iban if iban else None,
            bic=bic if bic else None,
            bank_country=bank_country
        )

        if not counterparty_result.get('success'):
            return {"success": False, "error": counterparty_result.get('error', 'Failed to create counterparty')}

        # Save counterparty ID to partner's payout_details
        payout_details['revolut_counterparty_id'] = counterparty_result.get('counterparty_id')

        client.table('partners').update({
            'payout_details': payout_details,
            'payout_method': 'revolut'
        }).eq('id', partner_id).execute()

        print(f"[Revolut] Counterparty created for {partner.get('name')}: {counterparty_result.get('counterparty_id')}")

        return {
            "success": True,
            "counterparty_id": counterparty_result.get('counterparty_id'),
            "partner_name": partner.get('name', '')
        }

    except Exception as e:
        print(f"Admin create counterparty error: {e}")
        import traceback
        traceback.print_exc()
        return {"success": False, "error": str(e)}


@router.post("/api/v1/admin/send-revolut-payout")
async def admin_send_revolut_payout(request: Request, password: str):
    """Send Revolut payout to a single partner"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from datetime import datetime
        from prometheus_backend.revolut_service import get_revolut_service

        data = await request.json()
        partner_id = data.get('partner_id')

        if not partner_id:
            return {"success": False, "error": "Partner ID required"}

        client = get_supabase_client()

        # Get partner info
        partner_result = client.table('partners').select('*').eq('id', partner_id).execute()
        if not partner_result.data:
            return {"success": False, "error": "Partner not found"}

        partner = partner_result.data[0]
        payout_details = partner.get('payout_details', {}) or {}
        counterparty_id = payout_details.get('revolut_counterparty_id', '')

        if not counterparty_id:
            return {"success": False, "error": "Partner has no Revolut counterparty. Create one first."}

        # Get confirmed referrals
        referrals_result = client.table('partner_referrals')\
            .select('*')\
            .eq('partner_id', partner_id)\
            .eq('commission_status', 'confirmed')\
            .is_('payout_id', 'null')\
            .execute()

        referrals = referrals_result.data or []

        if not referrals:
            return {"success": False, "error": "No confirmed referrals to pay"}

        total_amount = sum(float(r.get('commission_amount', 0)) for r in referrals)

        if total_amount < 50:
            return {"success": False, "error": f"Amount €{total_amount:.2f} below €50 minimum"}

        # Send Revolut payout
        revolut_service = get_revolut_service()

        if not revolut_service.configured:
            return {"success": False, "error": "Revolut not configured. Set REVOLUT_ACCESS_TOKEN."}

        payout_result = await revolut_service.send_payout(
            counterparty_id=counterparty_id,
            amount=total_amount,
            currency="EUR",
            reference=f"Naya Commission - {partner.get('referral_code', '')}"
        )

        if not payout_result.get('success'):
            return {"success": False, "error": payout_result.get('error', 'Revolut payout failed')}

        # Create payout record
        payout_data = {
            'partner_id': partner_id,
            'amount': total_amount,
            'currency': 'EUR',
            'referral_count': len(referrals),
            'period_start': referrals[-1].get('created_at', '')[:10] if referrals else '',
            'period_end': referrals[0].get('created_at', '')[:10] if referrals else '',
            'status': 'completed',
            'payout_method': 'revolut',
            'payout_reference': payout_result.get('transfer_id', ''),
            'completed_at': datetime.now().isoformat()
        }

        payout_insert = client.table('partner_payouts').insert(payout_data).execute()
        payout_id = payout_insert.data[0]['id'] if payout_insert.data else None

        # Mark referrals as paid
        for r in referrals:
            client.table('partner_referrals').update({
                'payout_id': payout_id,
                'commission_status': 'paid',
                'paid_at': datetime.now().isoformat()
            }).eq('id', r['id']).execute()

        print(f"[Revolut] Payout sent: €{total_amount:.2f} to {partner.get('name')} (transfer: {payout_result.get('transfer_id')})")

        return {
            "success": True,
            "amount": total_amount,
            "currency": "EUR",
            "referral_count": len(referrals),
            "transfer_id": payout_result.get('transfer_id'),
            "partner_name": partner.get('name', '')
        }

    except Exception as e:
        print(f"Admin Revolut payout error: {e}")
        import traceback
        traceback.print_exc()
        return {"success": False, "error": str(e)}


@router.post("/api/v1/admin/send-batch-revolut-payouts")
async def admin_send_batch_revolut_payouts(request: Request, password: str):
    """Send Revolut payouts to all eligible partners"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from datetime import datetime
        from prometheus_backend.revolut_service import get_revolut_service

        client = get_supabase_client()
        revolut_service = get_revolut_service()

        if not revolut_service.configured:
            return {"success": False, "error": "Revolut not configured. Set REVOLUT_ACCESS_TOKEN."}

        # Get pending payouts data
        pending_data = await admin_get_pending_payouts(password)
        pending_payouts = pending_data.get('pending_payouts', [])

        # Filter eligible payouts (must have counterparty_id)
        eligible = [p for p in pending_payouts if p['eligible'] and p.get('counterparty_id')]

        if not eligible:
            return {"success": False, "error": "No eligible payouts found. Ensure partners have Revolut counterparties created."}

        # Prepare batch payouts
        batch_items = []
        for p in eligible:
            batch_items.append({
                'counterparty_id': p['counterparty_id'],
                'amount': p['total_amount'],
                'partner_id': p['partner_id'],
                'name': p['partner_name'],
                'currency': 'EUR'
            })

        # Send batch payout
        batch_result = await revolut_service.send_batch_payouts(batch_items)

        # Create payout records and mark referrals as paid for successful payouts
        payouts_created = 0
        for result in batch_result.get('results', []):
            if not result.get('success'):
                continue

            partner_id = result.get('partner_id')

            # Get confirmed referrals for this partner
            referrals_result = client.table('partner_referrals')\
                .select('*')\
                .eq('partner_id', partner_id)\
                .eq('commission_status', 'confirmed')\
                .is_('payout_id', 'null')\
                .execute()

            referrals = referrals_result.data or []

            if referrals:
                # Create payout record
                payout_data = {
                    'partner_id': partner_id,
                    'amount': result.get('amount', 0),
                    'currency': 'EUR',
                    'referral_count': len(referrals),
                    'period_start': referrals[-1].get('created_at', '')[:10],
                    'period_end': referrals[0].get('created_at', '')[:10],
                    'status': 'completed',
                    'payout_method': 'revolut',
                    'payout_reference': result.get('transfer_id', ''),
                    'completed_at': datetime.now().isoformat()
                }

                payout_insert = client.table('partner_payouts').insert(payout_data).execute()
                payout_id = payout_insert.data[0]['id'] if payout_insert.data else None

                # Mark referrals as paid
                for r in referrals:
                    client.table('partner_referrals').update({
                        'payout_id': payout_id,
                        'commission_status': 'paid',
                        'paid_at': datetime.now().isoformat()
                    }).eq('id', r['id']).execute()

                payouts_created += 1

        print(f"[Revolut] Batch payout sent: {batch_result.get('successful', 0)}/{batch_result.get('total_payouts', 0)} partners")

        return {
            "success": batch_result.get('failed', 0) == 0,
            "payouts_sent": payouts_created,
            "successful": batch_result.get('successful', 0),
            "failed": batch_result.get('failed', 0),
            "total_amount": batch_result.get('total_amount', 0)
        }

    except Exception as e:
        print(f"Admin batch Revolut payout error: {e}")
        import traceback
        traceback.print_exc()
        return {"success": False, "error": str(e)}


@router.post("/api/v1/admin/confirm-pending-commissions")
async def admin_confirm_pending_commissions(password: str):
    """Confirm all pending commissions that are older than 14 days (hold period passed)"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from datetime import datetime, timedelta
        client = get_supabase_client()

        # Calculate cutoff date (14 days ago)
        cutoff_date = (datetime.now() - timedelta(days=14)).isoformat()

        # Get pending referrals older than 14 days
        result = client.table('partner_referrals')\
            .select('id')\
            .eq('commission_status', 'pending')\
            .lt('created_at', cutoff_date)\
            .execute()

        referrals = result.data or []

        if not referrals:
            return {"success": True, "confirmed": 0, "message": "No pending commissions to confirm"}

        # Update status to confirmed
        for r in referrals:
            client.table('partner_referrals').update({
                'commission_status': 'confirmed',
                'confirmed_at': datetime.now().isoformat()
            }).eq('id', r['id']).execute()

        print(f"[Admin] Confirmed {len(referrals)} pending commissions")

        return {"success": True, "confirmed": len(referrals)}

    except Exception as e:
        print(f"Admin confirm commissions error: {e}")
        return {"success": False, "error": str(e)}