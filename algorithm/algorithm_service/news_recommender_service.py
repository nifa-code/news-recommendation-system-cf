# news_recommender/algorithm_service/news_recommender_service.py
"""
新闻推荐服务主程序 - 完整版
定时从Java后端获取数据，训练模型，生成推荐并发送回Java
"""
import os
import sys
import time
from datetime import datetime
import requests
import schedule
from improved_user_cf_api import ImprovedUserCFWithAPI
sys.path.append(os.path.dirname(os.path.abspath(__file__)))


class NewsRecommenderService:
    """新闻推荐服务"""
    def __init__(self, java_api_url="http://localhost:8080", model_dir="models",
                 enable_api=False, api_port=5000):
        self.java_api_url = java_api_url
        self.model_dir = model_dir
        self.current_model = None
        self.enable_api = enable_api
        self.api_port = api_port

        # 确保模型目录存在
        os.makedirs(model_dir, exist_ok=True)

        print(f"🔧 初始化新闻推荐服务")
        print(f"   Java API: {java_api_url}")
        print(f"   模型目录: {model_dir}")
        print(f"   API服务: {'启用' if enable_api else '禁用'}")

        # 如果启用API，在另一个线程启动Flask服务
        if enable_api:
            self.start_api_service()

    def get_recommendations_for_user(self, user_id: str, top_k: int = 10) -> dict:
        """
        为指定用户获取推荐（供Java调用）
        """
        if self.current_model is None:
            if not self.load_latest_model():
                return {
                    "success": False,
                    "error": "没有可用的推荐模型",
                    "recommendations": []
                }

        try:
            # 生成推荐
            recommendations = self.current_model.recommend(user_id, top_k=top_k)

            # 转换为Java友好的格式
            rec_list = []
            for news_id, score in recommendations:
                rec_list.append({
                    "newsId": str(news_id),
                    "score": float(score)
                })

            return {
                "success": True,
                "userId": user_id,
                "recommendations": rec_list,
                "count": len(rec_list),
                "generatedAt": datetime.now().isoformat()
            }

        except Exception as e:
            return {
                "success": False,
                "userId": user_id,
                "error": str(e),
                "recommendations": []
            }
    def start_api_service(self):
        """启动API服务"""
        from threading import Thread

        def run_api():
            # 这里可以直接运行上面的recommendation_api.py
            # 为了简化，我们可以在内部启动Flask
            from flask import Flask, jsonify, request

            api_app = Flask(__name__)

            @api_app.route('/api/health', methods=['GET'])
            def health():
                return jsonify({"status": "ok", "service": "recommender"})

            @api_app.route('/api/recommend/<user_id>', methods=['GET'])
            def recommend(user_id):
                if self.current_model is None:
                    return jsonify({"error": "模型未加载"}), 400

                try:
                    # 直接调用模型的推荐方法
                    recommendations = self.current_model.recommend(user_id, top_k=10)

                    rec_list = [
                        {"newsId": str(news_id), "score": float(score)}
                        for news_id, score in recommendations
                    ]

                    return jsonify({
                        "success": True,
                        "userId": user_id,
                        "recommendations": rec_list
                    })
                except Exception as e:
                    return jsonify({
                        "success": False,
                        "error": str(e)
                    }), 500

            print(f"🚀 启动推荐API，端口: {self.api_port}")
            api_app.run(host='0.0.0.0', port=self.api_port, debug=False, use_reloader=False)

        # 在新线程中启动API
        api_thread = Thread(target=run_api, daemon=True)
        api_thread.start()
        time.sleep(2)  # 给API启动时间

    def train_model(self, days: int = 7) -> bool:
        """训练推荐模型"""
        print(f"\n🎯 开始训练推荐模型...")
        try:
            # 创建推荐器
            recommender = ImprovedUserCFWithAPI(
                java_api_url=self.java_api_url,
                k_neighbors=20,
                min_similarity=0.1
            )
            # 获取数据并训练
            print(f"   从Java获取最近{days}天数据...")
            recommender = recommender.fit_from_java_api(days=days)
            if recommender is None or recommender.user_index is None:
                print("❌ 训练失败：未获取到有效数据")
                return False
            # 保存模型
            model_filename = f"user_cf_model_{datetime.now().strftime('%Y%m%d_%H%M%S')}.pkl"
            model_path = os.path.join(self.model_dir, model_filename)
            recommender.save_model(model_path)
            # 更新当前模型
            self.current_model = recommender
            # 也保存为latest.pkl用于快速访问
            latest_path = os.path.join(self.model_dir, "user_cf_latest.pkl")
            recommender.save_model(latest_path)
            print(f"✅ 模型训练完成!")
            print(f"   用户数: {len(recommender.user_index)}")
            print(f"   新闻数: {len(recommender.item_index)}")
            print(f"   模型保存: {model_path}")
            return True
        except Exception as e:
            print(f"❌ 模型训练失败: {str(e)}")
            import traceback
            traceback.print_exc()
            return False
    def generate_recommendations(self, user_ids=None, top_k=10, send_to_java=True):
        """生成推荐"""
        if self.current_model is None:
            print("❌ 没有可用的模型，请先训练模型")
            return False

        print(f"\n🎯 开始生成推荐...")

        try:
            # 如果没有指定用户，则使用模型中的用户
            if user_ids is None:
                user_ids = list(self.current_model.user_index.keys())

            # 限制数量，避免太多请求
            max_users = min(50, len(user_ids))
            user_ids_to_recommend = user_ids[:max_users]

            print(f"   为 {len(user_ids_to_recommend)} 个用户生成推荐...")

            recommended_count = 0
            failed_count = 0

            for i, user_id in enumerate(user_ids_to_recommend, 1):
                try:
                    # 生成推荐
                    recommendations = self.current_model.recommend(
                        user_id, top_k=top_k, diversify=True
                    )

                    if recommendations:
                        if send_to_java:
                            # 发送到Java后端
                            success = self.current_model.send_recommendations_to_java(
                                user_id, recommendations
                            )
                            if success:
                                recommended_count += 1
                            else:
                                failed_count += 1
                        else:
                            # 仅打印，不发送
                            print(f"\n   用户 {user_id} 的推荐:")
                            for news_id, score in recommendations[:3]:  # 只显示前3个
                                print(f"     - {news_id}: {score:.4f}")
                            recommended_count += 1

                        # 显示进度
                        if i % 10 == 0:
                            print(f"      进度: {i}/{len(user_ids_to_recommend)}")

                    # 避免请求过快
                    time.sleep(0.05)

                except Exception as e:
                    print(f"   用户 {user_id} 推荐失败: {str(e)}")
                    failed_count += 1

            print(f"\n✅ 推荐生成完成!")
            print(f"   成功: {recommended_count} 个用户")
            print(f"   失败: {failed_count} 个用户")

            return recommended_count > 0

        except Exception as e:
            print(f"❌ 生成推荐失败: {str(e)}")
            return False

    def get_active_users_from_java(self, hours=24, limit=100):
        """从Java获取活跃用户列表"""
        try:
            # 这里假设Java有活跃用户API
            # 如果没有，可以使用模拟数据
            url = f"{self.java_api_url}/api/v1/ml/active-users"
            params = {"hours": hours, "limit": limit}

            response = requests.get(url, params=params, timeout=10)

            if response.status_code == 200:
                data = response.json()
                return data.get("userIds", [])
            else:
                print(f"⚠️  获取活跃用户失败，使用模拟数据")
                # 返回一些模拟用户ID
                return [f"user_{i}" for i in range(1, 21)]

        except Exception as e:
            print(f"⚠️  获取活跃用户失败: {str(e)}，使用模拟数据")
            return [f"user_{i}" for i in range(1, 21)]

    def load_latest_model(self):
        """加载最新的模型"""
        latest_path = os.path.join(self.model_dir, "user_cf_latest.pkl")

        if os.path.exists(latest_path):
            try:
                self.current_model = ImprovedUserCFWithAPI.load_model(latest_path)
                print(f"✅ 加载最新模型: {latest_path}")
                print(f"   用户数: {len(self.current_model.user_index)}")
                return True
            except Exception as e:
                print(f"❌ 加载模型失败: {str(e)}")
                return False
        else:
            print("⚠️  没有找到最新模型文件")
            return False

    def run_scheduled_job(self):
        """执行定时任务"""
        print(f"\n{'=' * 60}")
        print(f"执行定时任务: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"{'=' * 60}")

        # 1. 训练模型
        success = self.train_model(days=7)

        if success:
            # 2. 生成推荐
            active_users = self.get_active_users_from_java(hours=24, limit=100)
            self.generate_recommendations(
                user_ids=active_users,
                top_k=10,
                send_to_java=True
            )

        print(f"\n⏱️  任务完成时间: {datetime.now().strftime('%H:%M:%S')}")

    def start(self, initial_train=True):
        """启动推荐服务"""
        print("🚀 启动新闻推荐服务")
        print(f"启动时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"{'=' * 60}")

        # 初始训练（可选）
        if initial_train:
            print("执行初始训练...")
            self.run_scheduled_job()

        # 设置定时任务
        # 每天凌晨2点执行
        schedule.every().day.at("02:00").do(self.run_scheduled_job)

        # 每天下午2点执行
        schedule.every().day.at("14:00").do(self.run_scheduled_job)

        # 每6小时执行（用于测试）
        # schedule.every(6).hours.do(self.run_scheduled_job)

        print("\n📅 定时任务已设置:")
        print("   - 每天 02:00")
        print("   - 每天 14:00")
        print(f"\n⏳ 服务运行中，等待定时任务...")

        # 保持程序运行
        try:
            while True:
                schedule.run_pending()
                time.sleep(60)  # 每分钟检查一次
        except KeyboardInterrupt:
            print("\n👋 服务已停止")

    def get_recommendations_for_user(self, user_id: str, top_k: int = 10) -> dict:
        """
        为指定用户获取推荐（供Java调用）
        """
        if self.current_model is None:
            if not self.load_latest_model():
                return {
                    "success": False,
                    "error": "没有可用的推荐模型",
                    "recommendations": []
                }

        try:
            # 生成推荐
            recommendations = self.current_model.recommend(user_id, top_k=top_k)

            # 转换为Java友好的格式
            rec_list = []
            for news_id, score in recommendations:
                rec_list.append({
                    "newsId": str(news_id),
                    "score": float(score),
                    "type": "recommendation"  # 可添加更多元数据
                })

            return {
                "success": True,
                "userId": user_id,
                "recommendations": rec_list,
                "count": len(rec_list),
                "generatedAt": datetime.now().isoformat()
            }

        except Exception as e:
            return {
                "success": False,
                "userId": user_id,
                "error": str(e),
                "recommendations": []
            }


def main():
    """主函数"""
    import argparse

    parser = argparse.ArgumentParser(description="新闻推荐服务")
    parser.add_argument("--java-url", default="http://localhost:8080", help="Java后端地址")
    parser.add_argument("--model-dir", default="models", help="模型保存目录")
    parser.add_argument("--no-initial-train", action="store_true", help="跳过初始训练")

    args = parser.parse_args()

    # 创建服务实例
    service = NewsRecommenderService(
        java_api_url=args.java_url,
        model_dir=args.model_dir
    )

    # 启动服务
    service.start(initial_train=not args.no_initial_train)


if __name__ == "__main__":
    main()