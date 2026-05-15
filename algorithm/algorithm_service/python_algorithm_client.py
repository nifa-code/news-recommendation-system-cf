# python_algorithm_client.py
import requests
import pandas as pd
from typing import List, Dict
import json
from improved_user_cf_api import ImprovedUserCFWithAPI

class JavaDataClient:
    """从Java后端获取用户行为数据的客户端"""

    def __init__(self, base_url="http://localhost:8080"):
        self.base_url = base_url

    def fetch_behavior_data(self, days: int = 7) -> pd.DataFrame:
        """获取用户行为数据"""
        url = f"{self.base_url}/api/v1/ml/behavior-data?days={days}"

        response = requests.get(url, timeout=30)
        if response.status_code == 200:
            data = response.json()
            behaviors = data['behaviors']

            # 转换为DataFrame
            df = pd.DataFrame(behaviors)

            # 将行为转换为评分
            df['rating'] = df['behaviorType'].apply(self._behavior_to_rating)

            print(f"从Java后端获取到 {len(df)} 条行为记录")
            print(f"用户数: {data['userCount']}, 新闻数: {data['newsCount']}")

            return df[['userId', 'newsId', 'rating']]
        else:
            print(f"获取数据失败: {response.status_code}")
            return pd.DataFrame()

    def _behavior_to_rating(self, behavior_type: str) -> float:
        """将行为类型转换为评分"""
        rating_map = {
            "VIEW": 1.0,
            "CLICK": 1.5,
            "LIKE": 2.0,
            "COLLECT": 3.0
        }
        return rating_map.get(behavior_type, 1.0)

    def send_recommendations(self, user_id: str, recommendations: List[Dict]):
        """将推荐结果发送回Java后端"""
        url = f"{self.base_url}/api/v1/ml/recommendations"

        payload = {
            "userId": user_id,
            "recommendations": recommendations,
            "generatedAt": pd.Timestamp.now().isoformat()
        }

        response = requests.post(url, json=payload, timeout=10)
        return response.status_code == 200


# 在你的协同过滤算法中使用
def train_and_recommend():
    # 1. 从Java后端获取数据
    client = JavaDataClient()
    df = client.fetch_behavior_data(days=30)

    if df.empty:
        print("没有获取到数据，使用默认数据")
        return

    # 2. 训练模型
    cf_model = ImprovedUserCFWithAPI(k_neighbors=20)
    cf_model.fit(df)

    # 3. 为每个用户生成推荐
    user_ids = df['userId'].unique()
    for user_id in user_ids[:10]:  # 为前10个用户生成推荐
        recommendations = cf_model.recommend(user_id, top_k=10)

        # 4. 将推荐结果发送回Java
        if recommendations:
            rec_list = [{"newsId": news_id, "score": float(score)}
                        for news_id, score in recommendations]
            success = client.send_recommendations(user_id, rec_list)

            if success:
                print(f"用户 {user_id} 的推荐结果已发送到Java后端")
            else:
                print(f"用户 {user_id} 的推荐结果发送失败")