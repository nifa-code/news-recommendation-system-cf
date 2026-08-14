# news_recommender/algorithm_service/improved_user_cf_api.py
import pandas as pd
import numpy as np
from scipy.sparse import csr_matrix, lil_matrix
from sklearn.metrics.pairwise import cosine_similarity
from collections import Counter
import pickle
from typing import List, Tuple, Dict
import requests
import datetime
#在服务启动时，使用ImprovedUserCFWithAPI加载静态模型（作为基础模型），
# 然后从Java获取最新的数据，进行增量更新（或者重新训练）。
#或者，如果静态模型不存在，则从Java获取数据训练一个新模型。
class ImprovedUserCFWithAPI:
    """改进的基于用户的协同过滤推荐器"""

    def __init__(self, k_neighbors: int = 20, min_similarity: float = 0.1, java_api_url: str = None):
        """
        初始化推荐器

        参数:
            k_neighbors: 相似用户数量
            min_similarity: 最小相似度阈值
        """
        self.k_neighbors = k_neighbors
        self.min_similarity = min_similarity
        self.java_api_url = java_api_url

        # 模型参数
        self.user_sim_matrix = None
        self.user_item_matrix = None
        self.user_index = {}  # 用户ID -> 矩阵索引
        self.item_index = {}  # 物品ID -> 矩阵索引
        self.index_to_item = {}  # 矩阵索引 -> 物品ID
        self.item_popularity = {}  # 物品流行度统计
        self.user_history = {}  # 用户历史记录缓存

        print(f"初始化ImprovedUserCF: k={k_neighbors}, min_sim={min_similarity}")

    def fetch_data_from_java(self, days: int = 7) -> pd.DataFrame:
        """从Java后端API获取用户行为数据"""
        if not self.java_api_url:
            print("❌ 未设置Java API地址，无法获取数据")
            return pd.DataFrame()

        try:
            url = f"{self.java_api_url}/api/v1/ml/behavior-data"
            params = {"days": days}
            print(f"📡 从Java后端获取数据: {url}")
            response = requests.get(url, params=params, timeout=30)
            if response.status_code == 200:
                data = response.json()
                behaviors = data.get('behaviors', [])

                if not behaviors:
                    print("⚠️  获取的数据为空列表")
                    return pd.DataFrame()

                print(f"✅ 获取到 {len(behaviors)} 条行为记录")
                print(f"   用户数: {data.get('userCount', 0)}")
                print(f"   新闻数: {data.get('newsCount', 0)}")

                # 转换为DataFrame
                df = pd.DataFrame(behaviors)

                # 检查必要的列是否存在
                required_columns = ['userId', 'newsId', 'behaviorType']
                missing_columns = [col for col in required_columns if col not in df.columns]

                if missing_columns:
                    print(f"❌ 数据缺少必要的列: {missing_columns}")
                    print(f"   数据列: {list(df.columns)}")
                    return pd.DataFrame()

                # 将行为类型转换为评分
                def behavior_to_rating(behavior_type):
                    rating_map = {
                        "VIEW": 1.0,
                        "CLICK": 1.5,
                        "LIKE": 2.0,
                        "COLLECT": 3.0
                    }
                    return rating_map.get(behavior_type, 1.0)

                df['rating'] = df['behaviorType'].apply(behavior_to_rating)

                # 重命名列以匹配算法期望
                df = df.rename(columns={
                    'userId': 'user_id',
                    'newsId': 'news_id'
                })

                return df[['user_id', 'news_id', 'rating']]

            else:
                print(f"❌ 获取数据失败: HTTP {response.status_code}")
                if response.text:
                    print(f"   错误信息: {response.text[:200]}")
                return pd.DataFrame()

        except requests.exceptions.ConnectionError:
            print(f"❌ 连接Java后端失败，请确保Java服务正在运行: {self.java_api_url}")
            return pd.DataFrame()
        except Exception as e:
            print(f"❌ 获取数据时发生错误: {str(e)}")
            import traceback
            traceback.print_exc()
            return pd.DataFrame()

    def fit_from_java_api(self, days: int = 30,
                          min_user_interactions: int = 5,
                          min_item_interactions: int = 3) -> 'ImprovedUserCF':
        """
        从Java API获取数据并训练模型

        参数:
            days: 获取最近几天的数据
            min_user_interactions: 最小用户交互次数
            min_item_interactions: 最小物品交互次数
        """
        print("=" * 60)
        print("从Java API获取数据并训练模型")
        print("=" * 60)

        # 1. 从Java API获取数据
        interactions_df = self.fetch_data_from_java(days)

        if interactions_df.empty:
            print("❌ 未获取到数据，训练终止")
            return self

        print(f"✅ 获取到 {len(interactions_df)} 条数据")

        # 2. 使用原有的fit方法训练
        return self.fit(interactions_df, min_user_interactions, min_item_interactions)

    def send_recommendations_to_java(self, user_id: str, recommendations: List[Tuple[str, float]]) -> bool:
        if not self.java_api_url:
            print("❌ 未设置Java API地址，无法发送推荐结果")
            return False

        try:
            url = f"{self.java_api_url}/api/v1/ml/recommendations"

            # 格式化推荐结果
            rec_list = [{"newsId": news_id, "score": float(score)}
                        for news_id, score in recommendations]

            from datetime import datetime
            payload = {
                "userId": user_id,
                "recommendations": rec_list,
                "algorithm": "user_cf",
                "generatedAt": datetime.now().isoformat()
            }

            response = requests.post(url, json=payload, timeout=10)

            if response.status_code == 200:
                print(f"✅ 用户 {user_id} 的推荐结果已发送到Java")
                return True
            else:
                print(f"❌ 发送推荐结果失败: HTTP {response.status_code}")
                return False

        except Exception as e:
            print(f"❌ 发送推荐结果时发生错误: {str(e)}")
            return False

    def recommend_for_java_api(self, user_id: str, top_k: int = 10) -> dict:
        """
        为Java API接口准备的推荐方法
        返回字典格式，方便JSON序列化
        """
        try:
            recommendations = self.recommend(user_id, top_k=top_k, diversify=True)

            # 转换为Java端期望的格式
            rec_list = []
            for news_id, score in recommendations:
                rec_list.append({
                    "newsId": str(news_id),
                    "score": float(score),
                    "confidence": min(1.0, score / 3.0)  # 置信度，0-1之间
                })

            return {
                "success": True,
                "userId": user_id,
                "recommendations": rec_list,
                "generatedAt": datetime.now().isoformat(),
                "modelInfo": {
                    "userCount": len(self.user_index),
                    "itemCount": len(self.item_index)
                }
            }

        except Exception as e:
            return {
                "success": False,
                "userId": user_id,
                "error": str(e),
                "recommendations": []
            }

    def fit(self, interactions_df: pd.DataFrame,
            min_user_interactions: int = 5,
            min_item_interactions: int = 3) -> 'ImprovedUserCF':
        """
        训练推荐模型

        参数:
            interactions_df: 包含user_id, news_id, rating的DataFrame
            min_user_interactions: 最小用户交互次数
            min_item_interactions: 最小物品交互次数
        """
        print("=" * 50)
        print("开始训练协同过滤模型")
        print("=" * 50)

        # 1. 数据统计
        print(f"原始数据: {len(interactions_df)} 条交互")
        print(f"用户数: {interactions_df['user_id'].nunique()}")
        print(f"新闻数: {interactions_df['news_id'].nunique()}")

        # 2. 过滤稀疏数据
        filtered_df = self._filter_sparse_data(
            interactions_df, min_user_interactions, min_item_interactions
        )

        # 3. 构建用户-物品矩阵
        self._build_user_item_matrix(filtered_df)

        # 4. 计算用户相似度
        self._compute_user_similarities()

        # 5. 计算物品流行度
        self._compute_item_popularity(filtered_df)

        # 6. 缓存用户历史
        self._cache_user_history(filtered_df)

        print(f"\n模型训练完成!")
        print(f"- 有效用户: {len(self.user_index)}")
        print(f"- 有效物品: {len(self.item_index)}")
        print(f"- 用户相似度矩阵: {self.user_sim_matrix.shape}")

        return self

    def _filter_sparse_data(self, df, min_user, min_item):
        """过滤不活跃用户和冷门物品"""
        # 统计交互次数
        user_counts = df['user_id'].value_counts()
        item_counts = df['news_id'].value_counts()

        # 筛选
        active_users = user_counts[user_counts >= min_user].index
        popular_items = item_counts[item_counts >= min_item].index

        filtered = df[
            df['user_id'].isin(active_users) &
            df['news_id'].isin(popular_items)
            ].copy()

        print(f"\n数据过滤:")
        print(f"- 活跃用户(≥{min_user}次): {len(active_users)}")
        print(f"- 热门物品(≥{min_item}次): {len(popular_items)}")
        print(f"- 过滤后数据: {len(filtered)} 条 ({len(filtered) / len(df):.1%})")

        return filtered

    def _build_user_item_matrix(self, df):
        """构建用户-物品交互矩阵"""
        print("\n构建用户-物品矩阵...")
        column_mapping = {}
        if 'userId' in df.columns:
            column_mapping['userId'] = 'user_id'
        if 'newsId' in df.columns:
            column_mapping['newsId'] = 'news_id'
        if column_mapping:
            df = df.rename(columns=column_mapping)

        # 创建映射
        users = df['user_id'].unique()
        items = df['news_id'].unique()

        self.user_index = {u: i for i, u in enumerate(users)}
        self.item_index = {i: idx for idx, i in enumerate(items)}
        self.index_to_item = {idx: i for i, idx in self.item_index.items()}

        # 构建稀疏矩阵
        n_users = len(users)
        n_items = len(items)

        # 使用lil_matrix便于构建
        matrix = lil_matrix((n_users, n_items), dtype=np.float32)

        for _, row in df.iterrows():
            u_idx = self.user_index[row['user_id']]
            i_idx = self.item_index[row['news_id']]
            matrix[u_idx, i_idx] = row['rating']

        self.user_item_matrix = matrix.tocsr()
        print(f"- 矩阵形状: {self.user_item_matrix.shape}")
        print(f"- 稀疏度: {self.user_item_matrix.nnz / (n_users * n_items):.4%}")

    def _compute_user_similarities(self):
        """计算用户相似度矩阵"""
        print("\n计算用户相似度...")
        self.user_sim_matrix = cosine_similarity(self.user_item_matrix)

        # 将对角线设为0（自己与自己的相似度）
        np.fill_diagonal(self.user_sim_matrix, 0)

        # 添加微小噪声避免完全相同
        np.random.seed(42)
        noise = np.random.uniform(0, 1e-8, self.user_sim_matrix.shape)
        self.user_sim_matrix = np.maximum(0, self.user_sim_matrix + noise)

    def _compute_item_popularity(self, df):
        """计算物品流行度"""
        self.item_popularity = Counter(df['news_id'])
        print(f"- 物品流行度统计完成")

    def _cache_user_history(self, df):
        """缓存用户历史交互"""
        for user_id, group in df.groupby('user_id'):
            self.user_history[user_id] = set(group['news_id'].tolist())
        print(f"- 缓存了 {len(self.user_history)} 个用户的历史记录")

    def recommend(self, user_id: str, top_k: int = 10,
                  diversify: bool = True) -> List[Tuple[str, float]]:
        """
        为用户生成推荐

        参数:
            user_id: 用户ID
            top_k: 推荐数量
            diversify: 是否多样化推荐

        返回:
            推荐列表，每个元素为(物品ID, 推荐分数)
        """
        # 冷启动处理：新用户返回热门推荐
        if user_id not in self.user_index:
            return self._get_popular_recommendations(top_k)

        user_idx = self.user_index[user_id]

        # 获取相似用户
        similar_users = self._get_similar_users(user_idx)

        if not similar_users:
            return self._get_popular_recommendations(top_k)

        # 收集候选物品
        candidates = self._collect_candidates(user_idx, similar_users)

        if not candidates:
            return self._get_popular_recommendations(top_k)

        # 排序和多样化
        if diversify:
            recommendations = self._diversify_recommendations(candidates, top_k)
        else:
            recommendations = sorted(candidates.items(),
                                     key=lambda x: x[1], reverse=True)[:top_k]

        return recommendations

    def _get_similar_users(self, user_idx):
        """获取相似用户"""
        similarities = self.user_sim_matrix[user_idx]

        # 找到相似度大于阈值的用户
        similar_indices = np.where(similarities > self.min_similarity)[0]

        if len(similar_indices) == 0:
            return []

        # 获取相似度和索引
        similar_users = list(zip(similar_indices, similarities[similar_indices]))

        # 按相似度排序
        similar_users.sort(key=lambda x: x[1], reverse=True)

        # 取前k个
        return similar_users[:self.k_neighbors]

    def _collect_candidates(self, user_idx, similar_users):
        """从相似用户收集候选物品"""
        candidates = {}
        user_interacted = set(self.user_item_matrix[user_idx].nonzero()[1])

        for sim_user_idx, sim_score in similar_users:
            # 相似用户交互过的物品
            sim_user_items = self.user_item_matrix[sim_user_idx].nonzero()[1]

            for item_idx in sim_user_items:
                if item_idx not in user_interacted:  # 目标用户未交互
                    item_id = self.index_to_item[item_idx]

                    # 获取相似用户对该物品的评分
                    rating = self.user_item_matrix[sim_user_idx, item_idx]

                    # 计算推荐分数：相似度 × 评分 × 流行度加权
                    popularity = self.item_popularity.get(item_id, 0)
                    pop_weight = 0.3 + 0.7 * min(1.0, popularity / 50)  # 平滑处理

                    score = sim_score * rating * pop_weight

                    if item_id in candidates:
                        candidates[item_id] += score
                    else:
                        candidates[item_id] = score

        return candidates

    def _get_popular_recommendations(self, top_k):
        """获取热门推荐（用于冷启动）"""
        popular_items = sorted(self.item_popularity.items(),
                               key=lambda x: x[1], reverse=True)[:top_k * 2]

        # 归一化分数到[0, 1]
        if popular_items:
            max_pop = max(pop for _, pop in popular_items)
            return [(item, pop / max_pop) for item, pop in popular_items[:top_k]]
        return []

    def _diversify_recommendations(self, candidates, top_k):
        """多样化推荐"""
        sorted_items = sorted(candidates.items(), key=lambda x: x[1], reverse=True)

        if len(sorted_items) <= top_k:
            return sorted_items

        # 简单多样化：从高到低间隔选取
        diversified = []
        taken_indices = set()

        # 先取top 3确保质量
        for i in range(min(3, len(sorted_items))):
            diversified.append(sorted_items[i])
            taken_indices.add(i)

        # 间隔选取增加多样性
        step = max(2, len(sorted_items) // top_k)
        idx = 3
        while len(diversified) < top_k and idx < len(sorted_items):
            if idx not in taken_indices:
                diversified.append(sorted_items[idx])
            idx += step

        # 如果还不够，补足
        idx = 0
        while len(diversified) < top_k and idx < len(sorted_items):
            if idx not in taken_indices:
                diversified.append(sorted_items[idx])
            idx += 1

        return diversified[:top_k]

    def save_model(self, filepath: str):
        """保存模型到文件 - 修复版"""
        import os
        os.makedirs(os.path.dirname(filepath), exist_ok=True)

        # 关键：保存整个对象，而不是字典
        with open(filepath, 'wb') as f:
            pickle.dump(self, f)  # ← 直接保存self对象

        print(f"✅ 模型已保存到: {filepath} (文件大小: {os.path.getsize(filepath) / 1024 / 1024:.2f} MB)")

    @classmethod
    def load_model(cls, filepath: str) -> 'ImprovedUserCF':
        """从文件加载模型 - 修复版"""
        with open(filepath, 'rb') as f:
            model = pickle.load(f)  # ← 直接加载对象

        # 验证加载的对象类型
        if not isinstance(model, cls):
            print(f"⚠️  警告: 加载的文件不是 {cls.__name__} 类型，而是 {type(model).__name__}")
        print(f"✅ 模型已从 {filepath} 加载")
        return model