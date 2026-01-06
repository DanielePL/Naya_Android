"""
Revolut Business API Service - Automated partner commission payments
"""

import os
import uuid
import hmac
import hashlib
import time
import json
from datetime import datetime
from typing import List, Dict, Optional
import httpx


class RevolutPayoutService:
    """
    Service for sending payouts to partners via Revolut Business API.

    Setup:
    1. Create Revolut Business account (Company plan required for payouts)
    2. Go to https://business.revolut.com/settings/api
    3. Create API certificate and get credentials
    4. Set environment variables:
       - REVOLUT_ACCESS_TOKEN (or use refresh token flow)
       - REVOLUT_REFRESH_TOKEN
       - REVOLUT_CLIENT_ID
       - REVOLUT_MODE (sandbox or production)

    Note: Due to PSD2 regulations, payouts require Company plan.
    """

    SANDBOX_URL = "https://sandbox-b2b.revolut.com/api/1.0"
    PRODUCTION_URL = "https://b2b.revolut.com/api/1.0"

    def __init__(self):
        self.access_token = os.environ.get("REVOLUT_ACCESS_TOKEN")
        self.refresh_token = os.environ.get("REVOLUT_REFRESH_TOKEN")
        self.client_id = os.environ.get("REVOLUT_CLIENT_ID")
        self.mode = os.environ.get("REVOLUT_MODE", "sandbox")

        self.base_url = self.PRODUCTION_URL if self.mode == "production" else self.SANDBOX_URL

        if self.access_token:
            self.configured = True
            print(f"[Revolut] Configured in {self.mode} mode")
        else:
            self.configured = False
            print("[Revolut] Not configured - missing access token")

    def _get_headers(self) -> Dict:
        """Get authorization headers for API requests"""
        return {
            "Authorization": f"Bearer {self.access_token}",
            "Content-Type": "application/json"
        }

    async def get_accounts(self) -> Dict:
        """
        Get all business accounts to find source account for payouts.

        Returns:
            Dict with accounts list or error
        """
        if not self.configured:
            return {"success": False, "error": "Revolut not configured"}

        try:
            async with httpx.AsyncClient() as client:
                response = await client.get(
                    f"{self.base_url}/accounts",
                    headers=self._get_headers(),
                    timeout=30.0
                )

                if response.status_code == 200:
                    accounts = response.json()
                    return {"success": True, "accounts": accounts}
                else:
                    return {
                        "success": False,
                        "error": f"API error: {response.status_code}",
                        "details": response.text
                    }
        except Exception as e:
            print(f"[Revolut] Exception getting accounts: {e}")
            return {"success": False, "error": str(e)}

    async def create_counterparty(
        self,
        name: str,
        email: str = None,
        iban: str = None,
        bic: str = None,
        bank_country: str = None,
        currency: str = "EUR"
    ) -> Dict:
        """
        Create a counterparty (recipient) for payouts.

        Can use either:
        - Revolut account (by email/phone)
        - External bank account (IBAN + BIC)

        Args:
            name: Recipient's full name
            email: Revolut account email (for Revolut-to-Revolut transfers)
            iban: Bank account IBAN (for bank transfers)
            bic: Bank BIC/SWIFT code
            bank_country: ISO country code (e.g., "DE", "AT")
            currency: Currency code

        Returns:
            Dict with counterparty details or error
        """
        if not self.configured:
            return {"success": False, "error": "Revolut not configured"}

        # Build counterparty payload
        if email and not iban:
            # Revolut-to-Revolut transfer (free, instant)
            payload = {
                "profile_type": "personal",
                "name": name,
                "email": email
            }
        elif iban:
            # Bank transfer (may have fees)
            payload = {
                "profile_type": "personal",
                "name": name,
                "bank_country": bank_country or "DE",
                "currency": currency,
                "iban": iban
            }
            if bic:
                payload["bic"] = bic
        else:
            return {"success": False, "error": "Either email or IBAN required"}

        try:
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    f"{self.base_url}/counterparty",
                    headers=self._get_headers(),
                    json=payload,
                    timeout=30.0
                )

                if response.status_code in [200, 201]:
                    counterparty = response.json()
                    print(f"[Revolut] Counterparty created: {counterparty.get('id')}")
                    return {
                        "success": True,
                        "counterparty_id": counterparty.get("id"),
                        "counterparty": counterparty
                    }
                else:
                    print(f"[Revolut] Create counterparty failed: {response.text}")
                    return {
                        "success": False,
                        "error": f"API error: {response.status_code}",
                        "details": response.text
                    }
        except Exception as e:
            print(f"[Revolut] Exception: {e}")
            return {"success": False, "error": str(e)}

    async def get_counterparties(self) -> Dict:
        """Get all existing counterparties"""
        if not self.configured:
            return {"success": False, "error": "Revolut not configured"}

        try:
            async with httpx.AsyncClient() as client:
                response = await client.get(
                    f"{self.base_url}/counterparties",
                    headers=self._get_headers(),
                    timeout=30.0
                )

                if response.status_code == 200:
                    return {"success": True, "counterparties": response.json()}
                else:
                    return {
                        "success": False,
                        "error": f"API error: {response.status_code}"
                    }
        except Exception as e:
            return {"success": False, "error": str(e)}

    async def send_payout(
        self,
        counterparty_id: str,
        amount: float,
        currency: str = "EUR",
        account_id: str = None,
        reference: str = "Prometheus Partner Commission",
        request_id: str = None
    ) -> Dict:
        """
        Send a payout to a counterparty.

        Args:
            counterparty_id: The Revolut counterparty ID
            amount: Amount to send
            currency: Currency code (EUR, USD, etc.)
            account_id: Source account ID (if None, uses default account)
            reference: Payment reference/description
            request_id: Unique request ID for idempotency

        Returns:
            Dict with transfer details or error
        """
        if not self.configured:
            return {"success": False, "error": "Revolut not configured"}

        if not request_id:
            request_id = f"PROM-{uuid.uuid4().hex[:16].upper()}"

        # If no account_id provided, get the first account with matching currency
        if not account_id:
            accounts_result = await self.get_accounts()
            if not accounts_result.get("success"):
                return accounts_result

            for acc in accounts_result.get("accounts", []):
                if acc.get("currency") == currency and acc.get("balance", 0) >= amount:
                    account_id = acc.get("id")
                    break

            if not account_id:
                return {
                    "success": False,
                    "error": f"No account found with sufficient {currency} balance"
                }

        payload = {
            "request_id": request_id,
            "account_id": account_id,
            "receiver": {
                "counterparty_id": counterparty_id
            },
            "amount": amount,
            "currency": currency,
            "reference": reference
        }

        try:
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    f"{self.base_url}/pay",
                    headers=self._get_headers(),
                    json=payload,
                    timeout=30.0
                )

                if response.status_code in [200, 201, 204]:
                    transfer = response.json() if response.text else {}
                    print(f"[Revolut] Payout sent: {transfer.get('id', 'success')}")
                    return {
                        "success": True,
                        "transfer_id": transfer.get("id"),
                        "state": transfer.get("state", "created"),
                        "request_id": request_id,
                        "transfer": transfer
                    }
                else:
                    print(f"[Revolut] Payout failed: {response.text}")
                    return {
                        "success": False,
                        "error": f"API error: {response.status_code}",
                        "details": response.text
                    }
        except Exception as e:
            print(f"[Revolut] Exception: {e}")
            return {"success": False, "error": str(e)}

    async def send_batch_payouts(
        self,
        payouts: List[Dict],
        reference_prefix: str = "Prometheus Commission"
    ) -> Dict:
        """
        Send multiple payouts sequentially.

        Note: Revolut doesn't have a native batch API like PayPal,
        so we process each payout individually with error handling.

        Args:
            payouts: List of dicts with keys:
                - counterparty_id: Revolut counterparty ID
                - amount: Amount to send
                - currency: Currency code (default EUR)
                - partner_id: Partner UUID (for reference)
                - name: Partner name (for reference)
            reference_prefix: Prefix for payment reference

        Returns:
            Dict with results for each payout
        """
        if not self.configured:
            return {"success": False, "error": "Revolut not configured"}

        if not payouts:
            return {"success": False, "error": "No payouts to process"}

        results = []
        successful = 0
        failed = 0
        total_amount = 0

        for payout in payouts:
            counterparty_id = payout.get("counterparty_id")
            amount = payout.get("amount", 0)
            currency = payout.get("currency", "EUR")
            partner_name = payout.get("name", "Partner")
            partner_id = payout.get("partner_id", "")

            reference = f"{reference_prefix} - {partner_name}"
            request_id = f"PROM-{partner_id[:8] if partner_id else uuid.uuid4().hex[:8]}"

            result = await self.send_payout(
                counterparty_id=counterparty_id,
                amount=amount,
                currency=currency,
                reference=reference,
                request_id=request_id
            )

            result["partner_id"] = partner_id
            result["amount"] = amount
            results.append(result)

            if result.get("success"):
                successful += 1
                total_amount += amount
            else:
                failed += 1

        return {
            "success": failed == 0,
            "total_payouts": len(payouts),
            "successful": successful,
            "failed": failed,
            "total_amount": total_amount,
            "results": results
        }

    async def get_transfer_status(self, transfer_id: str) -> Dict:
        """
        Get status of a specific transfer.

        Args:
            transfer_id: The Revolut transfer ID

        Returns:
            Dict with transfer status
        """
        if not self.configured:
            return {"success": False, "error": "Revolut not configured"}

        try:
            async with httpx.AsyncClient() as client:
                response = await client.get(
                    f"{self.base_url}/transaction/{transfer_id}",
                    headers=self._get_headers(),
                    timeout=30.0
                )

                if response.status_code == 200:
                    transfer = response.json()
                    return {
                        "success": True,
                        "transfer_id": transfer_id,
                        "state": transfer.get("state"),
                        "created_at": transfer.get("created_at"),
                        "completed_at": transfer.get("completed_at"),
                        "transfer": transfer
                    }
                else:
                    return {
                        "success": False,
                        "error": f"API error: {response.status_code}"
                    }
        except Exception as e:
            return {"success": False, "error": str(e)}


# Singleton instance
_revolut_service = None

def get_revolut_service() -> RevolutPayoutService:
    """Get or create Revolut service instance"""
    global _revolut_service
    if _revolut_service is None:
        _revolut_service = RevolutPayoutService()
    return _revolut_service