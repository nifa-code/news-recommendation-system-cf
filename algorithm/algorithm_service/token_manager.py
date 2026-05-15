# token_manager.py
import requests
import time
import json
import os
from datetime import datetime, timedelta


class TokenManager:
    """JWT Token管理器 - 自动缓存和刷新"""

    def __init__(self, java_url, username="ml_service", password="ml_password"):
        self.java_url = java_url
        self.username = username
        self.password = password
        self.token = None
        self.token_expiry = None
        self.token_file = ".ml_token_cache.json"  # 缓存文件

        # 尝试从缓存加载
        self._load_from_cache()

    def _load_from_cache(self):
        """从缓存文件加载Token"""
        try:
            if os.path.exists(self.token_file):
                with open(self.token_file, 'r') as f:
                    cache = json.load(f)
                    self.token = cache.get('token')

                    # 检查是否过期
                    expiry_str = cache.get('expiry')
                    if expiry_str:
                        self.token_expiry = datetime.fromisoformat(expiry_str)

                    # 如果Token还有效
                    if self.token and self.token_expiry and datetime.now() < self.token_expiry:
                        print("✅ 从缓存加载有效Token")
                        return True

        except Exception as e:
            print(f"⚠️  加载缓存失败: {e}")

        return False

    def _save_to_cache(self, token, expiry_hours=1):
        """保存Token到缓存"""
        try:
            self.token_expiry = datetime.now() + timedelta(hours=expiry_hours)

            cache = {
                'token': token,
                'expiry': self.token_expiry.isoformat(),
                'saved_at': datetime.now().isoformat(),
                'username': self.username
            }

            with open(self.token_file, 'w') as f:
                json.dump(cache, f, indent=2)

            print(f"✅ Token已缓存，有效期至: {self.token_expiry}")

        except Exception as e:
            print(f"⚠️  缓存Token失败: {e}")

    def _fetch_new_token(self):
        """从Java后端获取新Token"""
        try:
            # 尝试多种可能的登录端点
            login_endpoints = [
                "/api/auth/login",
                "/api/auth/token",
                "/api/login",
                "/auth/login"
            ]

            for endpoint in login_endpoints:
                try:
                    login_url = f"{self.java_url}{endpoint}"
                    payload = {
                        "username": self.username,
                        "password": self.password
                    }

                    print(f"🔑 尝试登录: {login_url}")
                    response = requests.post(login_url, json=payload, timeout=10)

                    if response.status_code == 200:
                        data = response.json()
                        token = data.get('token') or data.get('accessToken') or data.get('jwt')

                        if token:
                            print("✅ 获取新Token成功")
                            return token

                except Exception as e:
                    continue

            # 如果所有端点都失败
            print("❌ 所有登录接口尝试失败")
            return None

        except Exception as e:
            print(f"❌ 获取Token异常: {e}")
            return None

    def get_valid_token(self, force_refresh=False):
        """
        获取有效的Token
        force_refresh: 强制刷新，忽略缓存
        """
        # 1. 如果强制刷新，直接获取新Token
        if force_refresh:
            print("🔄 强制刷新Token...")
            self.token = self._fetch_new_token()
            if self.token:
                self._save_to_cache(self.token)
            return self.token

        # 2. 检查缓存是否有效
        if self.token and self.token_expiry:
            # 提前5分钟刷新，避免边缘情况
            refresh_time = self.token_expiry - timedelta(minutes=5)

            if datetime.now() < refresh_time:
                return self.token  # Token仍有效
            else:
                print(f"🔄 Token即将过期，自动刷新...")

        # 3. 获取新Token
        self.token = self._fetch_new_token()
        if self.token:
            self._save_to_cache(self.token)

        return self.token

    def get_auth_headers(self):
        """获取带有认证头的headers字典"""
        token = self.get_valid_token()
        if token:
            return {
                'Authorization': f'Bearer {token}',
                'Content-Type': 'application/json',
                'User-Agent': 'NewsRecommender/1.0'
            }
        else:
            return {'Content-Type': 'application/json'}

# 使用示例
# token_manager = TokenManager("http://localhost:8080", "ml_service", "your_password")
# headers = token_manager.get_auth_headers()