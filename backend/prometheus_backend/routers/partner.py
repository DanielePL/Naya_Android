"""
Partner Portal Router - Dashboard for partners to view their earnings
"""

from fastapi import APIRouter, HTTPException, Request, Header
from fastapi.responses import HTMLResponse
from typing import Optional
from datetime import datetime
import os
import hashlib

router = APIRouter(tags=["Partner Portal"])


def get_supabase_client():
    """Get Supabase client"""
    from supabase import create_client
    supabase_url = os.environ.get("SUPABASE_URL")
    supabase_key = os.environ.get("SUPABASE_KEY")
    return create_client(supabase_url, supabase_key)


async def get_partner_by_code(referral_code: str):
    """Get partner data by referral code (for session verification)"""
    try:
        client = get_supabase_client()
        result = client.table('partners').select('*').eq('referral_code', referral_code.upper()).eq('status', 'active').execute()

        if result.data and len(result.data) > 0:
            return result.data[0]
        return None
    except Exception as e:
        print(f"Partner lookup error: {e}")
        return None


# ============================================================
# PARTNER PORTAL HTML
# ============================================================

PARTNER_LOGIN_HTML = """
<!DOCTYPE html>
<html>
<head>
    <title>Prometheus Partner Portal</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        * { box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            display: flex; justify-content: center; align-items: center;
            min-height: 100vh; margin: 0; background: #0f0f0f; color: #fff;
            padding: 20px;
        }
        .container {
            background: #1a1a1a; padding: 40px; border-radius: 16px;
            box-shadow: 0 4px 30px rgba(0,0,0,0.4); width: 100%; max-width: 450px;
        }
        .logo { text-align: center; margin-bottom: 25px; }
        .logo h1 { color: #ff6b35; margin: 0; font-size: 26px; }
        .logo p { color: #666; margin: 10px 0 0 0; font-size: 14px; }

        /* Tabs */
        .tabs { display: flex; margin-bottom: 25px; border-radius: 10px; overflow: hidden; background: #222; }
        .tab { flex: 1; padding: 14px; text-align: center; cursor: pointer; font-weight: 600; font-size: 14px; transition: all 0.2s; color: #888; }
        .tab:hover { color: #fff; }
        .tab.active { background: linear-gradient(135deg, #ff6b35 0%, #f7931e 100%); color: #fff; }

        .form-group { margin-bottom: 18px; }
        .form-group label { display: block; color: #888; margin-bottom: 6px; font-size: 13px; }
        .form-group .hint { color: #555; font-size: 11px; margin-top: 4px; }
        input {
            width: 100%; padding: 14px 16px; font-size: 16px;
            border: 1px solid #333; border-radius: 10px;
            background: #2a2a2a; color: #fff;
        }
        input:focus { outline: none; border-color: #ff6b35; }
        input::placeholder { color: #555; }
        button {
            width: 100%; padding: 14px 24px; font-size: 16px;
            background: linear-gradient(135deg, #ff6b35 0%, #f7931e 100%);
            color: #fff; border: none; border-radius: 10px;
            cursor: pointer; font-weight: 600; margin-top: 10px;
        }
        button:hover { opacity: 0.9; }
        button:disabled { opacity: 0.5; cursor: not-allowed; }
        .error {
            background: #3f1a1a; color: #ff6b6b; padding: 12px;
            border-radius: 8px; margin-bottom: 20px; display: none;
            font-size: 14px;
        }
        .success {
            background: #1a3f1a; color: #6bff6b; padding: 16px;
            border-radius: 8px; margin-bottom: 20px; display: none;
            font-size: 14px; text-align: center;
        }
        .success .code {
            font-size: 24px; font-weight: bold; color: #ff6b35;
            margin: 10px 0; letter-spacing: 2px;
        }
        .panel { display: none; }
        .panel.active { display: block; }

        .password-requirements {
            background: #252525; border-radius: 8px; padding: 10px 12px;
            margin-top: 8px; font-size: 11px;
        }
        .password-requirements div { color: #666; margin-bottom: 4px; }
        .password-requirements div.valid { color: #4ade80; }
        .password-requirements div::before { content: '○ '; }
        .password-requirements div.valid::before { content: '● '; }
    </style>
</head>
<body>
    <div class="container">
        <div class="logo">
            <h1>Partner Portal</h1>
            <p>Prometheus Affiliate Program</p>
        </div>

        <div class="tabs">
            <div class="tab active" onclick="showPanel('login')">Login</div>
            <div class="tab" onclick="showPanel('signup')">Sign Up</div>
        </div>

        <div class="error" id="error-msg"></div>
        <div class="success" id="success-msg">
            <div>Welcome to the Prometheus Partner Program!</div>
            <div>Your referral code is:</div>
            <div class="code" id="new-code"></div>
            <div style="margin-top: 15px; color: #888;">You can now login with your code and password.</div>
        </div>

        <!-- Login Panel -->
        <div id="login-panel" class="panel active">
            <form id="login-form">
                <div class="form-group">
                    <label>Referral Code</label>
                    <input type="text" id="login-code" placeholder="e.g., ALEX15" required
                           style="text-transform: uppercase;">
                </div>
                <div class="form-group">
                    <label>Password</label>
                    <input type="password" id="login-password" placeholder="Your partner password" required>
                </div>
                <button type="submit">Login to Dashboard</button>
            </form>
        </div>

        <!-- Signup Panel -->
        <div id="signup-panel" class="panel">
            <form id="signup-form">
                <div class="form-group">
                    <label>Full Name *</label>
                    <input type="text" id="signup-name" placeholder="Your name" required>
                </div>
                <div class="form-group">
                    <label>Email Address *</label>
                    <input type="email" id="signup-email" placeholder="your@email.com" required>
                </div>
                <div class="form-group">
                    <label>Social Media Handle *</label>
                    <input type="text" id="signup-social" placeholder="@username or channel URL" required>
                    <div class="hint">Instagram, YouTube, TikTok, Twitter, etc.</div>
                </div>
                <div class="form-group">
                    <label>Create Password *</label>
                    <input type="password" id="signup-password" placeholder="Min. 8 characters" required minlength="8">
                    <div class="password-requirements">
                        <div id="pw-length">At least 8 characters</div>
                        <div id="pw-letter">Contains a letter</div>
                        <div id="pw-number">Contains a number</div>
                    </div>
                </div>
                <div class="form-group">
                    <label>Confirm Password *</label>
                    <input type="password" id="signup-password-confirm" placeholder="Repeat password" required>
                </div>
                <button type="submit" id="signup-btn">Create Partner Account</button>
            </form>
        </div>
    </div>

    <script>
        function showPanel(panel) {
            document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
            document.getElementById('error-msg').style.display = 'none';

            if (panel === 'login') {
                document.querySelector('.tab:first-child').classList.add('active');
                document.getElementById('login-panel').classList.add('active');
            } else {
                document.querySelector('.tab:last-child').classList.add('active');
                document.getElementById('signup-panel').classList.add('active');
            }
        }

        // Login form
        document.getElementById('login-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            const code = document.getElementById('login-code').value.toUpperCase();
            const password = document.getElementById('login-password').value;
            const errorEl = document.getElementById('error-msg');

            try {
                const res = await fetch('/api/v1/partner/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ referral_code: code, password: password })
                });
                const data = await res.json();

                if (data.success) {
                    window.location.href = '/partner?code=' + code;
                } else {
                    errorEl.textContent = data.error || 'Invalid credentials';
                    errorEl.style.display = 'block';
                }
            } catch (err) {
                errorEl.textContent = 'Connection error. Please try again.';
                errorEl.style.display = 'block';
            }
        });

        // Password validation
        const pwInput = document.getElementById('signup-password');
        pwInput.addEventListener('input', function() {
            const pw = this.value;
            document.getElementById('pw-length').classList.toggle('valid', pw.length >= 8);
            document.getElementById('pw-letter').classList.toggle('valid', /[a-zA-Z]/.test(pw));
            document.getElementById('pw-number').classList.toggle('valid', /[0-9]/.test(pw));
        });

        // Signup form
        document.getElementById('signup-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            const name = document.getElementById('signup-name').value.trim();
            const email = document.getElementById('signup-email').value.trim();
            const social = document.getElementById('signup-social').value.trim();
            const password = document.getElementById('signup-password').value;
            const passwordConfirm = document.getElementById('signup-password-confirm').value;
            const errorEl = document.getElementById('error-msg');
            const successEl = document.getElementById('success-msg');
            const submitBtn = document.getElementById('signup-btn');

            errorEl.style.display = 'none';

            if (password !== passwordConfirm) {
                errorEl.textContent = 'Passwords do not match';
                errorEl.style.display = 'block';
                return;
            }

            if (password.length < 8 || !/[a-zA-Z]/.test(password) || !/[0-9]/.test(password)) {
                errorEl.textContent = 'Password must be at least 8 characters with letters and numbers';
                errorEl.style.display = 'block';
                return;
            }

            submitBtn.disabled = true;
            submitBtn.textContent = 'Creating account...';

            try {
                const res = await fetch('/api/v1/partner/signup', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ name, email, social_handle: social, password })
                });
                const data = await res.json();

                if (data.success) {
                    document.getElementById('signup-panel').style.display = 'none';
                    document.querySelector('.tabs').style.display = 'none';
                    document.getElementById('new-code').textContent = data.referral_code;
                    successEl.style.display = 'block';
                } else {
                    errorEl.textContent = data.error || 'Sign up failed';
                    errorEl.style.display = 'block';
                    submitBtn.disabled = false;
                    submitBtn.textContent = 'Create Partner Account';
                }
            } catch (err) {
                errorEl.textContent = 'Connection error. Please try again.';
                errorEl.style.display = 'block';
                submitBtn.disabled = false;
                submitBtn.textContent = 'Create Partner Account';
            }
        });
    </script>
</body>
</html>
"""


def get_partner_dashboard_html(partner: dict) -> str:
    """Generate partner dashboard HTML"""
    partner_name = partner.get('name', 'Partner')
    partner_code = partner.get('referral_code', '')
    commission_percent = partner.get('commission_percent', 15)

    return f"""
    <!DOCTYPE html>
    <html>
    <head>
        <title>Partner Dashboard - {partner_name}</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
            * {{ box-sizing: border-box; }}
            body {{
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                margin: 0; padding: 15px; background: #0f0f0f; color: #e0e0e0;
            }}
            .header {{
                display: flex; justify-content: space-between; align-items: center;
                margin-bottom: 25px; padding-bottom: 20px; border-bottom: 1px solid #333;
                flex-wrap: wrap; gap: 15px;
            }}
            .header h1 {{ color: #ff6b35; margin: 0; font-size: 22px; }}
            .header-right {{ display: flex; align-items: center; gap: 15px; }}
            .code-badge {{
                background: linear-gradient(135deg, #ff6b35 0%, #f7931e 100%);
                padding: 8px 16px; border-radius: 20px; font-weight: bold;
                font-size: 14px;
            }}
            .logout {{
                color: #888; text-decoration: none; font-size: 14px;
                padding: 8px 16px; background: #222; border-radius: 8px;
            }}
            .logout:hover {{ background: #333; }}

            .grid {{
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
                gap: 12px; margin-bottom: 25px;
            }}
            .card {{
                background: #1a1a1a; border-radius: 12px; padding: 18px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.2);
            }}
            .card.highlight {{
                background: linear-gradient(135deg, #1a2a1a 0%, #1a1a1a 100%);
                border: 1px solid #2a5a2a;
            }}
            .card.pending {{
                background: linear-gradient(135deg, #2a2a1a 0%, #1a1a1a 100%);
                border: 1px solid #5a5a2a;
            }}
            .card h2 {{
                color: #888; margin: 0 0 10px 0; font-size: 11px;
                text-transform: uppercase; letter-spacing: 0.5px;
            }}
            .stat {{ font-size: 26px; font-weight: bold; color: #fff; }}
            .stat.green {{ color: #4ade80; }}
            .stat.yellow {{ color: #fbbf24; }}
            .stat.small {{ font-size: 20px; }}
            .stat-sub {{ color: #666; font-size: 11px; margin-top: 4px; }}

            .section {{ margin-bottom: 30px; }}
            .section-title {{
                color: #ff6b35; font-size: 16px; margin-bottom: 15px;
                padding-bottom: 10px; border-bottom: 1px solid #333;
                display: flex; align-items: center; gap: 10px;
            }}

            .table-wrap {{ overflow-x: auto; }}
            .table {{
                width: 100%; border-collapse: collapse; font-size: 13px;
                min-width: 500px;
            }}
            .table th, .table td {{
                padding: 12px 10px; text-align: left;
                border-bottom: 1px solid #2a2a2a;
            }}
            .table th {{
                color: #888; font-weight: 600; font-size: 11px;
                text-transform: uppercase; background: #151515;
            }}
            .table tr:hover {{ background: #1f1f1f; }}

            .badge {{
                display: inline-block; padding: 4px 10px; border-radius: 12px;
                font-size: 10px; font-weight: 600; text-transform: uppercase;
            }}
            .badge.pending {{ background: #3f3f00; color: #fbbf24; }}
            .badge.confirmed {{ background: #064e3b; color: #4ade80; }}
            .badge.paid {{ background: #1e3a5f; color: #60a5fa; }}
            .badge.cancelled {{ background: #450a0a; color: #ef4444; }}

            .promo-box {{
                background: linear-gradient(135deg, #1a1a2e 0%, #1a1a1a 100%);
                border: 1px solid #333; border-radius: 12px; padding: 20px;
                margin-bottom: 25px;
            }}
            .promo-box h3 {{ color: #60a5fa; margin: 0 0 12px 0; font-size: 14px; }}
            .promo-link {{
                background: #222; padding: 12px 15px; border-radius: 8px;
                font-family: monospace; font-size: 13px; color: #4ade80;
                word-break: break-all; margin: 10px 0;
            }}
            .copy-btn {{
                background: #333; color: #fff; border: none; padding: 8px 16px;
                border-radius: 6px; cursor: pointer; font-size: 12px; margin-top: 10px;
            }}
            .copy-btn:hover {{ background: #444; }}

            .payout-info {{
                background: #1a1a1a; border-radius: 12px; padding: 20px;
                border: 1px solid #333;
            }}
            .payout-info h3 {{ color: #ff6b35; margin: 0 0 15px 0; }}
            .payout-row {{
                display: flex; justify-content: space-between;
                padding: 10px 0; border-bottom: 1px solid #2a2a2a;
            }}
            .payout-row:last-child {{ border-bottom: none; }}
            .payout-label {{ color: #888; }}
            .payout-value {{ color: #fff; font-weight: 500; }}

            .empty-state {{
                text-align: center; padding: 40px 20px; color: #666;
            }}
            .empty-state .icon {{ font-size: 48px; margin-bottom: 15px; }}

            @media (max-width: 600px) {{
                .header {{ flex-direction: column; align-items: flex-start; }}
                .grid {{ grid-template-columns: repeat(2, 1fr); }}
                .stat {{ font-size: 22px; }}
            }}
        </style>
    </head>
    <body>
        <div class="header">
            <h1>Partner Dashboard</h1>
            <div class="header-right">
                <div class="code-badge">{partner_code}</div>
                <a href="/partner" class="logout">Logout</a>
            </div>
        </div>

        <div class="section">
            <div class="grid">
                <div class="card highlight">
                    <h2>Total Earnings</h2>
                    <div class="stat green" id="total-earnings">$0.00</div>
                    <div class="stat-sub">Lifetime commissions</div>
                </div>
                <div class="card pending">
                    <h2>Pending Payout</h2>
                    <div class="stat yellow" id="pending-payout">$0.00</div>
                    <div class="stat-sub">Ready to be paid</div>
                </div>
                <div class="card">
                    <h2>Total Referrals</h2>
                    <div class="stat small" id="total-referrals">0</div>
                    <div class="stat-sub">All time</div>
                </div>
                <div class="card">
                    <h2>This Month</h2>
                    <div class="stat small" id="month-referrals">0</div>
                    <div class="stat-sub">New referrals</div>
                </div>
                <div class="card">
                    <h2>Commission Rate</h2>
                    <div class="stat small">{commission_percent}%</div>
                    <div class="stat-sub">Per subscription</div>
                </div>
                <div class="card">
                    <h2>Conversion Rate</h2>
                    <div class="stat small" id="conversion-rate">0%</div>
                    <div class="stat-sub">Code uses to subs</div>
                </div>
            </div>
        </div>

        <div class="promo-box">
            <h3>Your Referral Link</h3>
            <p style="color: #888; font-size: 13px; margin: 0 0 10px 0;">
                Share this link or tell your followers to use code <strong style="color: #ff6b35;">{partner_code}</strong> at checkout
            </p>
            <div class="promo-link" id="promo-link">https://prometheus.fitness/download?ref={partner_code}</div>
            <button class="copy-btn" onclick="copyLink()">Copy Link</button>
            <button class="copy-btn" onclick="copyCode()" style="margin-left: 8px;">Copy Code: {partner_code}</button>
        </div>

        <div class="section">
            <div class="section-title">Recent Referrals</div>
            <div class="card">
                <div class="table-wrap">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Subscription</th>
                                <th>Amount</th>
                                <th>Your Commission</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody id="referrals-table">
                            <tr><td colspan="5" class="empty-state">
                                <div class="icon"></div>
                                Loading referrals...
                            </td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <div class="section">
            <div class="section-title">Payout History</div>
            <div class="card">
                <div class="table-wrap">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Period</th>
                                <th>Referrals</th>
                                <th>Amount</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody id="payouts-table">
                            <tr><td colspan="5" class="empty-state">
                                <div class="icon"></div>
                                No payouts yet
                            </td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <div class="section">
            <div class="section-title">Payout Settings</div>
            <div class="payout-info">
                <div style="margin-bottom: 20px;">
                    <p style="color: #888; font-size: 13px; margin: 0 0 15px 0;">
                        Choose your preferred payout method. Revolut email transfers are free and instant!
                    </p>

                    <!-- Revolut Email (preferred - free & instant) -->
                    <div style="margin-bottom: 15px;">
                        <label style="color: #4ade80; font-size: 13px; display: block; margin-bottom: 8px;">
                            Revolut Email (Free & Instant)
                        </label>
                        <input type="email" id="revolut-email" placeholder="your@revolut.email"
                               style="width: 100%; padding: 12px; background: #2a2a2a; border: 1px solid #333; border-radius: 8px; color: #fff; font-size: 14px; margin-bottom: 5px;">
                        <div style="color: #666; font-size: 11px;">If you have a Revolut account, enter your Revolut email for free instant transfers</div>
                    </div>

                    <!-- OR divider -->
                    <div style="text-align: center; margin: 20px 0; color: #555; font-size: 12px;">
                        — OR use bank transfer —
                    </div>

                    <!-- IBAN (bank transfer) -->
                    <div style="margin-bottom: 15px;">
                        <label style="color: #888; font-size: 13px; display: block; margin-bottom: 8px;">
                            IBAN (Bank Transfer)
                        </label>
                        <input type="text" id="iban" placeholder="DE89 3704 0044 0532 0130 00"
                               style="width: 100%; padding: 12px; background: #2a2a2a; border: 1px solid #333; border-radius: 8px; color: #fff; font-size: 14px; font-family: monospace;">
                    </div>

                    <!-- BIC (optional) -->
                    <div style="margin-bottom: 15px;">
                        <label style="color: #888; font-size: 13px; display: block; margin-bottom: 8px;">
                            BIC/SWIFT (Optional)
                        </label>
                        <input type="text" id="bic" placeholder="COBADEFFXXX"
                               style="width: 100%; padding: 12px; background: #2a2a2a; border: 1px solid #333; border-radius: 8px; color: #fff; font-size: 14px; font-family: monospace;">
                    </div>

                    <!-- Bank Country -->
                    <div style="margin-bottom: 15px;">
                        <label style="color: #888; font-size: 13px; display: block; margin-bottom: 8px;">
                            Bank Country
                        </label>
                        <select id="bank-country" style="width: 100%; padding: 12px; background: #2a2a2a; border: 1px solid #333; border-radius: 8px; color: #fff; font-size: 14px;">
                            <option value="DE">Germany (DE)</option>
                            <option value="AT">Austria (AT)</option>
                            <option value="CH">Switzerland (CH)</option>
                            <option value="NL">Netherlands (NL)</option>
                            <option value="FR">France (FR)</option>
                            <option value="IT">Italy (IT)</option>
                            <option value="ES">Spain (ES)</option>
                            <option value="GB">United Kingdom (GB)</option>
                            <option value="PL">Poland (PL)</option>
                            <option value="BE">Belgium (BE)</option>
                        </select>
                    </div>

                    <button onclick="savePayoutDetails()" style="width: 100%; padding: 14px 20px; background: #ff6b35; color: #fff; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: 15px;">Save Payout Details</button>
                    <div id="payout-save-status" style="font-size: 12px; margin-top: 8px; text-align: center;"></div>
                </div>
                <div class="payout-row">
                    <span class="payout-label">Minimum Payout</span>
                    <span class="payout-value">€50.00</span>
                </div>
                <div class="payout-row">
                    <span class="payout-label">Payout Schedule</span>
                    <span class="payout-value">Monthly (1st of month)</span>
                </div>
                <div class="payout-row">
                    <span class="payout-label">Commission Hold Period</span>
                    <span class="payout-value">14 days (for refunds)</span>
                </div>
                <div class="payout-row">
                    <span class="payout-label">Payout Method</span>
                    <span class="payout-value" id="payout-method">Revolut / Bank Transfer</span>
                </div>
            </div>
        </div>

        <script>
            const partnerCode = '{partner_code}';

            function copyLink() {{
                navigator.clipboard.writeText(document.getElementById('promo-link').textContent);
                alert('Link copied to clipboard!');
            }}

            function copyCode() {{
                navigator.clipboard.writeText(partnerCode);
                alert('Code copied: ' + partnerCode);
            }}

            async function loadPartnerData() {{
                try {{
                    const res = await fetch(`/api/v1/partner/stats?code=${{partnerCode}}`);
                    const data = await res.json();

                    document.getElementById('total-earnings').textContent = '$' + (data.total_earnings || 0).toFixed(2);
                    document.getElementById('pending-payout').textContent = '$' + (data.pending_payout || 0).toFixed(2);
                    document.getElementById('total-referrals').textContent = data.total_referrals || 0;
                    document.getElementById('month-referrals').textContent = data.month_referrals || 0;
                    document.getElementById('conversion-rate').textContent = (data.conversion_rate || 0).toFixed(1) + '%';
                    document.getElementById('payout-method').textContent = data.payout_method || 'Not configured';

                    if (data.recent_referrals && data.recent_referrals.length > 0) {{
                        document.getElementById('referrals-table').innerHTML = data.recent_referrals.map(r => `
                            <tr>
                                <td>${{r.date}}</td>
                                <td>${{r.subscription_type}}</td>
                                <td>${{r.amount}}</td>
                                <td style="color: #4ade80; font-weight: 600;">${{r.commission}}</td>
                                <td><span class="badge ${{r.status.toLowerCase()}}">${{r.status}}</span></td>
                            </tr>
                        `).join('');
                    }} else {{
                        document.getElementById('referrals-table').innerHTML = `
                            <tr><td colspan="5" class="empty-state">
                                <div class="icon"></div>
                                No referrals yet. Share your code to start earning!
                            </td></tr>
                        `;
                    }}

                    if (data.payouts && data.payouts.length > 0) {{
                        document.getElementById('payouts-table').innerHTML = data.payouts.map(p => `
                            <tr>
                                <td>${{p.date}}</td>
                                <td>${{p.period}}</td>
                                <td>${{p.referral_count}}</td>
                                <td style="color: #4ade80; font-weight: 600;">${{p.amount}}</td>
                                <td><span class="badge ${{p.status.toLowerCase()}}">${{p.status}}</span></td>
                            </tr>
                        `).join('');
                    }}

                }} catch (e) {{
                    console.error('Error loading partner data:', e);
                }}
            }}

            async function savePayoutDetails() {{
                const revolutEmail = document.getElementById('revolut-email').value.trim();
                const iban = document.getElementById('iban').value.trim().replace(/\\s/g, '');
                const bic = document.getElementById('bic').value.trim();
                const bankCountry = document.getElementById('bank-country').value;
                const statusEl = document.getElementById('payout-save-status');

                if (!revolutEmail && !iban) {{
                    statusEl.innerHTML = '<span style="color: #ff6b6b;">Please enter either a Revolut email or IBAN</span>';
                    return;
                }}

                statusEl.innerHTML = '<span style="color: #888;">Saving...</span>';

                try {{
                    const res = await fetch('/api/v1/partner/payout-details', {{
                        method: 'POST',
                        headers: {{ 'Content-Type': 'application/json' }},
                        body: JSON.stringify({{
                            code: partnerCode,
                            revolut_email: revolutEmail || null,
                            iban: iban || null,
                            bic: bic || null,
                            bank_country: bankCountry
                        }})
                    }});
                    const data = await res.json();

                    if (data.success) {{
                        statusEl.innerHTML = '<span style="color: #4ade80;">Payout details saved successfully!</span>';
                        document.getElementById('payout-method').textContent = data.payout_method || 'Revolut / Bank Transfer';
                    }} else {{
                        statusEl.innerHTML = '<span style="color: #ff6b6b;">' + (data.error || 'Failed to save') + '</span>';
                    }}
                }} catch (e) {{
                    statusEl.innerHTML = '<span style="color: #ff6b6b;">Connection error</span>';
                }}
            }}

            async function loadPayoutDetails() {{
                try {{
                    const res = await fetch(`/api/v1/partner/stats?code=${{partnerCode}}`);
                    const data = await res.json();
                    if (data.revolut_email) {{
                        document.getElementById('revolut-email').value = data.revolut_email;
                    }}
                    if (data.iban) {{
                        document.getElementById('iban').value = data.iban;
                    }}
                    if (data.bic) {{
                        document.getElementById('bic').value = data.bic;
                    }}
                    if (data.bank_country) {{
                        document.getElementById('bank-country').value = data.bank_country;
                    }}
                }} catch (e) {{}}
            }}

            loadPartnerData();
            loadPayoutDetails();
            setInterval(loadPartnerData, 60000);
        </script>
    </body>
    </html>
    """


# ============================================================
# PARTNER SIGN-UP HTML
# ============================================================

PARTNER_SIGNUP_HTML = """
<!DOCTYPE html>
<html>
<head>
    <title>Become a Partner - Prometheus</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        * { box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            display: flex; justify-content: center; align-items: center;
            min-height: 100vh; margin: 0; background: #0f0f0f; color: #fff;
            padding: 20px;
        }
        .signup-box {
            background: #1a1a1a; padding: 40px; border-radius: 16px;
            box-shadow: 0 4px 30px rgba(0,0,0,0.4); width: 100%; max-width: 450px;
        }
        .logo { text-align: center; margin-bottom: 30px; }
        .logo h1 { color: #ff6b35; margin: 0; font-size: 26px; }
        .logo p { color: #666; margin: 10px 0 0 0; font-size: 14px; }
        .form-group { margin-bottom: 18px; }
        .form-group label { display: block; color: #888; margin-bottom: 6px; font-size: 13px; }
        .form-group .hint { color: #555; font-size: 11px; margin-top: 4px; }
        input {
            width: 100%; padding: 14px 16px; font-size: 16px;
            border: 1px solid #333; border-radius: 10px;
            background: #2a2a2a; color: #fff;
        }
        input:focus { outline: none; border-color: #ff6b35; }
        input::placeholder { color: #555; }
        button {
            width: 100%; padding: 14px 24px; font-size: 16px;
            background: linear-gradient(135deg, #ff6b35 0%, #f7931e 100%);
            color: #fff; border: none; border-radius: 10px;
            cursor: pointer; font-weight: 600; margin-top: 10px;
        }
        button:hover { opacity: 0.9; }
        button:disabled { opacity: 0.5; cursor: not-allowed; }
        .error {
            background: #3f1a1a; color: #ff6b6b; padding: 12px;
            border-radius: 8px; margin-bottom: 20px; display: none;
            font-size: 14px;
        }
        .success {
            background: #1a3f1a; color: #6bff6b; padding: 16px;
            border-radius: 8px; margin-bottom: 20px; display: none;
            font-size: 14px; text-align: center;
        }
        .success .code {
            font-size: 24px; font-weight: bold; color: #ff6b35;
            margin: 10px 0; letter-spacing: 2px;
        }
        .benefits {
            background: #222; border-radius: 10px; padding: 16px;
            margin-bottom: 25px;
        }
        .benefits h3 { color: #ff6b35; margin: 0 0 12px 0; font-size: 14px; }
        .benefits ul { margin: 0; padding-left: 20px; }
        .benefits li { color: #aaa; font-size: 13px; margin-bottom: 6px; }
        .benefits li strong { color: #4ade80; }
        .help { text-align: center; margin-top: 25px; color: #666; font-size: 13px; }
        .help a { color: #ff6b35; text-decoration: none; }
        .password-requirements {
            background: #252525; border-radius: 8px; padding: 10px 12px;
            margin-top: 8px; font-size: 11px;
        }
        .password-requirements div { color: #666; margin-bottom: 4px; }
        .password-requirements div.valid { color: #4ade80; }
        .password-requirements div::before { content: '○ '; }
        .password-requirements div.valid::before { content: '● '; }
    </style>
</head>
<body>
    <div class="signup-box">
        <div class="logo">
            <h1>Become a Partner</h1>
            <p>Join the Prometheus Affiliate Program</p>
        </div>

        <div class="benefits">
            <h3>Partner Benefits</h3>
            <ul>
                <li><strong>15% commission</strong> on every subscription</li>
                <li>Recurring commissions on renewals</li>
                <li>Real-time dashboard tracking</li>
                <li>Monthly payouts via Revolut or bank transfer</li>
            </ul>
        </div>

        <div class="error" id="error-msg"></div>
        <div class="success" id="success-msg">
            <div>Welcome to the Prometheus Partner Program!</div>
            <div>Your referral code is:</div>
            <div class="code" id="new-code"></div>
            <div style="margin-top: 15px;">
                <a href="/partner" style="color: #ff6b35; font-weight: 600;">Login to your Dashboard →</a>
            </div>
        </div>

        <form id="signup-form">
            <div class="form-group">
                <label>Full Name *</label>
                <input type="text" id="name" placeholder="Your name" required>
            </div>
            <div class="form-group">
                <label>Email Address *</label>
                <input type="email" id="email" placeholder="your@email.com" required>
            </div>
            <div class="form-group">
                <label>Social Media Handle *</label>
                <input type="text" id="social-handle" placeholder="@username or channel URL" required>
                <div class="hint">Instagram, YouTube, TikTok, Twitter, etc. - We need this to verify your account</div>
            </div>
            <div class="form-group">
                <label>Create Password *</label>
                <input type="password" id="password" placeholder="Min. 8 characters" required minlength="8">
                <div class="password-requirements" id="pw-requirements">
                    <div id="pw-length">At least 8 characters</div>
                    <div id="pw-letter">Contains a letter</div>
                    <div id="pw-number">Contains a number</div>
                </div>
            </div>
            <div class="form-group">
                <label>Confirm Password *</label>
                <input type="password" id="password-confirm" placeholder="Repeat password" required>
            </div>
            <button type="submit" id="submit-btn">Create Partner Account</button>
        </form>

        <div class="help">
            Already have an account? <a href="/partner">Login here</a>
        </div>
    </div>

    <script>
        const passwordInput = document.getElementById('password');
        const pwLength = document.getElementById('pw-length');
        const pwLetter = document.getElementById('pw-letter');
        const pwNumber = document.getElementById('pw-number');

        passwordInput.addEventListener('input', function() {
            const pw = this.value;
            pwLength.classList.toggle('valid', pw.length >= 8);
            pwLetter.classList.toggle('valid', /[a-zA-Z]/.test(pw));
            pwNumber.classList.toggle('valid', /[0-9]/.test(pw));
        });

        document.getElementById('signup-form').addEventListener('submit', async (e) => {
            e.preventDefault();

            const name = document.getElementById('name').value.trim();
            const email = document.getElementById('email').value.trim();
            const socialHandle = document.getElementById('social-handle').value.trim();
            const password = document.getElementById('password').value;
            const passwordConfirm = document.getElementById('password-confirm').value;
            const errorEl = document.getElementById('error-msg');
            const successEl = document.getElementById('success-msg');
            const form = document.getElementById('signup-form');
            const submitBtn = document.getElementById('submit-btn');

            errorEl.style.display = 'none';

            // Validation
            if (password !== passwordConfirm) {
                errorEl.textContent = 'Passwords do not match';
                errorEl.style.display = 'block';
                return;
            }

            if (password.length < 8 || !/[a-zA-Z]/.test(password) || !/[0-9]/.test(password)) {
                errorEl.textContent = 'Password must be at least 8 characters with letters and numbers';
                errorEl.style.display = 'block';
                return;
            }

            submitBtn.disabled = true;
            submitBtn.textContent = 'Creating account...';

            try {
                const res = await fetch('/api/v1/partner/signup', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        name: name,
                        email: email,
                        social_handle: socialHandle,
                        password: password
                    })
                });

                const data = await res.json();

                if (data.success) {
                    form.style.display = 'none';
                    document.getElementById('new-code').textContent = data.referral_code;
                    successEl.style.display = 'block';
                } else {
                    errorEl.textContent = data.error || 'Sign up failed. Please try again.';
                    errorEl.style.display = 'block';
                    submitBtn.disabled = false;
                    submitBtn.textContent = 'Create Partner Account';
                }
            } catch (err) {
                errorEl.textContent = 'Connection error. Please try again.';
                errorEl.style.display = 'block';
                submitBtn.disabled = false;
                submitBtn.textContent = 'Create Partner Account';
            }
        });
    </script>
</body>
</html>
"""


# ============================================================
# PARTNER PORTAL ENDPOINTS
# ============================================================

@router.get("/partner/signup")
async def partner_signup_page():
    """Redirect to combined partner page"""
    from fastapi.responses import RedirectResponse
    return RedirectResponse(url="/partner", status_code=302)


@router.post("/api/v1/partner/signup")
async def partner_signup(request: Request):
    """Partner sign-up endpoint"""
    try:
        import random
        import string

        data = await request.json()
        name = data.get('name', '').strip()
        email = data.get('email', '').strip().lower()
        social_handle = data.get('social_handle', '').strip()
        password = data.get('password', '')

        # Validation
        if not name or not email or not password or not social_handle:
            return {"success": False, "error": "All fields are required (Name, Email, Social Media, Password)"}

        if len(password) < 8:
            return {"success": False, "error": "Password must be at least 8 characters"}

        if not any(c.isalpha() for c in password) or not any(c.isdigit() for c in password):
            return {"success": False, "error": "Password must contain letters and numbers"}

        client = get_supabase_client()

        # Check if email already exists
        existing = client.table('partners').select('id').eq('email', email).execute()
        if existing.data:
            return {"success": False, "error": "Email already registered. Please login instead."}

        # Generate unique referral code
        # Use first name + random numbers
        first_name = name.split()[0].upper()[:6]
        while True:
            random_suffix = ''.join(random.choices(string.digits, k=2))
            referral_code = f"{first_name}{random_suffix}"

            # Check if code exists
            code_exists = client.table('partners').select('id').eq('referral_code', referral_code).execute()
            if not code_exists.data:
                break

        # Hash password
        password_hash = hashlib.sha256(password.encode()).hexdigest()

        # Create partner
        partner_data = {
            "name": name,
            "email": email,
            "referral_code": referral_code,
            "commission_percent": 15,  # Default 15%
            "status": "pending",  # Pending until approved by admin
            "payout_method": None,
            "payout_details": {
                "password_hash": password_hash,
                "social_handle": social_handle
            },
            "created_at": datetime.now().isoformat()
        }

        result = client.table('partners').insert(partner_data).execute()

        if not result.data:
            return {"success": False, "error": "Failed to create account. Please try again."}

        print(f"[Partner Signup] New partner created: {name} ({email}) - Code: {referral_code}")

        return {
            "success": True,
            "referral_code": referral_code,
            "message": "Account created successfully"
        }

    except Exception as e:
        print(f"Partner signup error: {e}")
        import traceback
        traceback.print_exc()
        return {"success": False, "error": "Sign up failed. Please try again."}


@router.get("/partner")
async def partner_portal(code: Optional[str] = None):
    """
    Partner Portal - Dashboard for partners to view their earnings
    Access: /partner (login) or /partner?code=REFERRAL_CODE (authenticated)
    """
    partner = None
    if code:
        partner = await get_partner_by_code(code)

    if not partner:
        return HTMLResponse(content=PARTNER_LOGIN_HTML, status_code=200)

    return HTMLResponse(content=get_partner_dashboard_html(partner))


@router.post("/api/v1/partner/login")
async def partner_login(request: Request):
    """Partner login endpoint"""
    try:
        data = await request.json()
        referral_code = data.get('referral_code', '').upper()
        password = data.get('password', '')

        if not referral_code or not password:
            return {"success": False, "error": "Please enter code and password"}

        client = get_supabase_client()
        result = client.table('partners').select('*').eq('referral_code', referral_code).execute()

        if not result.data or len(result.data) == 0:
            return {"success": False, "error": "Invalid referral code"}

        partner = result.data[0]

        if partner.get('status') != 'active':
            return {"success": False, "error": "Account is not active"}

        stored_hash = partner.get('payout_details', {}).get('password_hash', '')
        input_hash = hashlib.sha256(password.encode()).hexdigest()

        if stored_hash != input_hash:
            return {"success": False, "error": "Invalid password"}

        return {"success": True, "partner_code": referral_code}

    except Exception as e:
        print(f"Partner login error: {e}")
        return {"success": False, "error": "Login failed"}


@router.get("/api/v1/partner/stats")
async def get_partner_stats(code: str):
    """Get partner statistics for their dashboard"""
    try:
        from datetime import datetime
        client = get_supabase_client()

        partner_result = client.table('partners').select('*').eq('referral_code', code.upper()).execute()

        if not partner_result.data:
            return {"error": "Partner not found"}

        partner = partner_result.data[0]
        partner_id = partner['id']

        referrals_result = client.table('partner_referrals').select('*').eq('partner_id', partner_id).order('created_at', desc=True).execute()
        referrals = referrals_result.data or []

        total_earnings = sum(float(r.get('commission_amount', 0)) for r in referrals)
        pending_payout = sum(float(r.get('commission_amount', 0)) for r in referrals if r.get('commission_status') in ['pending', 'confirmed'])

        month_start = datetime.now().replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        month_referrals = sum(1 for r in referrals if r.get('created_at') and r.get('created_at') >= month_start.isoformat())

        entries_result = client.table('referral_code_entries').select('*').eq('partner_id', partner_id).execute()
        total_entries = len(entries_result.data or [])
        conversion_rate = (len(referrals) / total_entries * 100) if total_entries > 0 else 0

        recent_referrals = []
        for r in referrals[:10]:
            recent_referrals.append({
                "date": r.get('created_at', '')[:10] if r.get('created_at') else '-',
                "subscription_type": r.get('subscription_type', 'subscription').replace('_', ' ').title(),
                "amount": f"${float(r.get('gross_amount', 0)):.2f}",
                "commission": f"${float(r.get('commission_amount', 0)):.2f}",
                "status": r.get('commission_status', 'pending').title()
            })

        payouts_result = client.table('partner_payouts').select('*').eq('partner_id', partner_id).order('created_at', desc=True).execute()
        payouts = []
        for p in (payouts_result.data or [])[:10]:
            payouts.append({
                "date": p.get('completed_at', p.get('created_at', ''))[:10] if p.get('completed_at') or p.get('created_at') else '-',
                "period": f"{p.get('period_start', '')} - {p.get('period_end', '')}",
                "referral_count": p.get('referral_count', 0),
                "amount": f"${float(p.get('amount', 0)):.2f}",
                "status": p.get('status', 'pending').title()
            })

        payout_method = partner.get('payout_method', 'Not configured')
        if payout_method:
            payout_method = payout_method.replace('_', ' ').title()

        # Get payout details (Revolut/IBAN)
        payout_details = partner.get('payout_details', {}) or {}
        revolut_email = payout_details.get('revolut_email', '')
        iban = payout_details.get('iban', '')
        bic = payout_details.get('bic', '')
        bank_country = payout_details.get('bank_country', 'DE')

        return {
            "total_earnings": total_earnings,
            "pending_payout": pending_payout,
            "total_referrals": len(referrals),
            "month_referrals": month_referrals,
            "conversion_rate": conversion_rate,
            "payout_method": payout_method,
            "revolut_email": revolut_email,
            "iban": iban,
            "bic": bic,
            "bank_country": bank_country,
            "recent_referrals": recent_referrals,
            "payouts": payouts
        }

    except Exception as e:
        print(f"Partner stats error: {e}")
        return {"error": str(e)}


@router.post("/api/v1/partner/payout-details")
async def save_payout_details(request: Request):
    """Save partner's payout details (Revolut email or IBAN)"""
    try:
        data = await request.json()
        code = data.get('code', '').upper()
        revolut_email = data.get('revolut_email', '').strip().lower() if data.get('revolut_email') else None
        iban = data.get('iban', '').strip().upper().replace(' ', '') if data.get('iban') else None
        bic = data.get('bic', '').strip().upper() if data.get('bic') else None
        bank_country = data.get('bank_country', 'DE').strip().upper()

        if not code:
            return {"success": False, "error": "Missing partner code"}

        if not revolut_email and not iban:
            return {"success": False, "error": "Please provide either Revolut email or IBAN"}

        # Validate Revolut email format
        if revolut_email and ('@' not in revolut_email or '.' not in revolut_email):
            return {"success": False, "error": "Invalid Revolut email format"}

        # Basic IBAN validation (length check)
        if iban and len(iban) < 15:
            return {"success": False, "error": "Invalid IBAN format"}

        client = get_supabase_client()

        # Get partner
        partner_result = client.table('partners').select('*').eq('referral_code', code).execute()

        if not partner_result.data:
            return {"success": False, "error": "Partner not found"}

        partner = partner_result.data[0]
        partner_id = partner['id']

        # Update payout_details with Revolut/IBAN info
        payout_details = partner.get('payout_details', {}) or {}

        # Clear old PayPal email if switching to Revolut
        if 'paypal_email' in payout_details:
            del payout_details['paypal_email']

        payout_details['revolut_email'] = revolut_email
        payout_details['iban'] = iban
        payout_details['bic'] = bic
        payout_details['bank_country'] = bank_country

        # Determine payout method
        if revolut_email:
            payout_method = "revolut"
        elif iban:
            payout_method = "bank_transfer"
        else:
            payout_method = None

        # Update partner
        client.table('partners').update({
            "payout_details": payout_details,
            "payout_method": payout_method,
            "updated_at": datetime.now().isoformat()
        }).eq('id', partner_id).execute()

        print(f"[Partner] Payout details saved for {code}: revolut={revolut_email}, iban={iban[:8] + '...' if iban else None}")

        return {
            "success": True,
            "payout_method": payout_method.replace('_', ' ').title() if payout_method else "Not configured"
        }

    except Exception as e:
        print(f"Save payout details error: {e}")
        import traceback
        traceback.print_exc()
        return {"success": False, "error": "Failed to save payout details"}