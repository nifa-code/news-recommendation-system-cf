# train_model.py
import mysql.connector
import pandas as pd
from algorithm_service.improved_user_cf_api import ImprovedUserCFWithAPI

# 数据库配置
DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "123456",
    "database": "news_recommender",
    "port": 3306
}

# 1. 从 MySQL 读取 user_behavior 数据
def load_behavior_from_db():
    conn = mysql.connector.connect(**DB_CONFIG)
    query = "SELECT user_id, news_id, behavior_type FROM user_behavior"
    df = pd.read_sql(query, conn)
    conn.close()

    # 转换成算法需要的 rating
    def behavior_to_rating(bt):
        m = {"VIEW":1.0, "CLICK":1.5, "LIKE":2.0, "COLLECT":3.0}
        return m.get(bt, 1.0)

    df["rating"] = df["behavior_type"].apply(behavior_to_rating)
    df.rename(columns={"news_id":"news_id"}, inplace=True)
    print(f"✅ 从数据库加载 {len(df)} 条行为数据")
    return df

# 2. 训练模型
if __name__ == "__main__":
    # 加载数据
    df = load_behavior_from_db()

    # 初始化模型
    cf = ImprovedUserCFWithAPI(k_neighbors=20, min_similarity=0.1)

    # 训练
    cf.fit(df)

    # 保存模型
    cf.save_model("models/user_cf_model.pkl")

    # 测试推荐（给 user001 推荐 10 条）
    print("\n🔥 测试推荐结果 for user001:")
    recs = cf.recommend("user001", top_k=10)
    for news_id, score in recs:
        print(f"新闻ID: {news_id}  推荐得分: {score:.2f}")