# recommendation_api.py
from flask import Flask, request, jsonify
import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from news_recommender_service import NewsRecommenderService
from improved_user_cf_api import ImprovedUserCFWithAPI

app = Flask(__name__)

# 全局推荐服务实例
recommender_service = None


def initialize_service(java_api_url: str, model_dir: str = "models"):
    """初始化推荐服务"""
    global recommender_service

    recommender_service = NewsRecommenderService(
        java_api_url=java_api_url,
        model_dir=model_dir
    )

    # 加载最新模型
    if not recommender_service.load_latest_model():
        print("⚠️  没有找到模型，开始训练初始模型...")
        recommender_service.train_model(days=7)

    print("✅ 推荐服务初始化完成")


@app.route('/api/health', methods=['GET'])
def health_check():
    """健康检查"""
    return jsonify({
        'status': 'ok',
        'service': 'news_recommender',
        'has_model': recommender_service.current_model is not None
    })


@app.route('/api/recommend/<user_id>', methods=['GET'])
def get_recommendations(user_id):
    """为指定用户生成推荐"""
    if recommender_service.current_model is None:
        return jsonify({
            'success': False,
            'error': '模型未加载，请先训练模型'
        }), 400

    try:
        # 生成推荐
        recommendations = recommender_service.current_model.recommend(
            user_id,
            top_k=10,
            diversify=True
        )

        # 转换为字典格式
        rec_list = [{"newsId": news_id, "score": float(score)}
                    for news_id, score in recommendations]

        return jsonify({
            'success': True,
            'userId': user_id,
            'recommendations': rec_list,
            'count': len(rec_list)
        })

    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@app.route('/api/recommend/batch', methods=['POST'])
def batch_recommendations():
    """批量生成推荐"""
    data = request.json
    user_ids = data.get('userIds', [])

    if not user_ids:
        return jsonify({
            'success': False,
            'error': '未提供用户ID列表'
        }), 400

    results = {}
    for user_id in user_ids:
        try:
            recommendations = recommender_service.current_model.recommend(
                user_id,
                top_k=10,
                diversify=True
            )
            results[user_id] = [
                {"newsId": news_id, "score": float(score)}
                for news_id, score in recommendations
            ]
        except Exception as e:
            results[user_id] = {
                'error': str(e),
                'recommendations': []
            }

    return jsonify({
        'success': True,
        'results': results
    })


@app.route('/api/train', methods=['POST'])
def train_model():
    """触发模型训练"""
    try:
        data = request.json
        days = data.get('days', 7)

        success = recommender_service.train_model(days)

        return jsonify({
            'success': success,
            'message': '模型训练完成' if success else '模型训练失败'
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


if __name__ == '__main__':
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument('--java-url', default='http://localhost:8080', help='Java后端地址')
    parser.add_argument('--model-dir', default='models', help='模型目录')
    parser.add_argument('--port', type=int, default=5000, help='API服务端口')

    args = parser.parse_args()

    # 初始化服务
    initialize_service(args.java_url, args.model_dir)

    print(f"🚀 启动推荐API服务，端口: {args.port}")
    print(f"📡 Java后端: {args.java_url}")
    print(f"💾 模型目录: {args.model_dir}")

    app.run(host='0.0.0.0', port=args.port, debug=False)