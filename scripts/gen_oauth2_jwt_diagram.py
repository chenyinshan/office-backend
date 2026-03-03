#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成 OAuth2.0 + JWT 流程图（中文无乱码）。
依赖: pip install matplotlib
运行: python scripts/gen_oauth2_jwt_diagram.py
输出: assets/oauth2_jwt_flow_diagram_cn.png
"""

import matplotlib.pyplot as plt
import os

# 使用支持中文的字体（macOS 常用 PingFang SC / 华文黑体，Windows 可用 SimHei / Microsoft YaHei）
def get_chinese_font():
    from matplotlib import font_manager
    candidates = [
        "PingFang SC",
        "Heiti SC",
        "STHeiti",
        "Microsoft YaHei",
        "SimHei",
        "WenQuanYi Micro Hei",
    ]
    for name in candidates:
        if any(f.name == name for f in font_manager.fontManager.ttflist):
            return name
    return None

font_name = get_chinese_font()
if font_name:
    plt.rcParams["font.sans-serif"] = [font_name]
plt.rcParams["axes.unicode_minus"] = False  # 解决负号显示为方框

def main():
    fig, ax = plt.subplots(1, 1, figsize=(10, 7))
    ax.set_xlim(0, 10)
    ax.set_ylim(0, 8)
    ax.axis("off")

    # 三个角色框
    box_style = dict(boxstyle="round,pad=0.4", facecolor="lightblue", edgecolor="gray", linewidth=1.5)
    ax.text(1.5, 4, "客户端\n(浏览器/APP)", ha="center", va="center", fontsize=11, bbox=box_style)
    ax.text(5, 4, "授权服务器\n(登录/发 Token)", ha="center", va="center", fontsize=11, bbox=box_style)
    ax.text(8.5, 4, "资源服务器\n(业务 API)", ha="center", va="center", fontsize=11, bbox=box_style)

    y = 6.5
    dy = 0.55
    # 步骤 1
    ax.annotate("", xy=(3.2, y), xytext=(1.8, y), arrowprops=dict(arrowstyle="->", color="green", lw=1.5))
    ax.text(2.5, y + 0.15, "1. 请求登录", fontsize=9, color="green", ha="center")
    y -= dy
    # 步骤 2
    ax.annotate("", xy=(1.8, y), xytext=(3.2, y), arrowprops=dict(arrowstyle="->", color="blue", lw=1.5))
    ax.text(2.5, y + 0.15, "2. 返回 JWT", fontsize=9, color="blue", ha="center")
    y -= dy
    # 步骤 3
    ax.annotate("", xy=(6.8, y), xytext=(5.2, y), arrowprops=dict(arrowstyle="->", color="green", lw=1.5))
    ax.text(6, y + 0.15, "3. 携带 Token 访问 API", fontsize=9, color="green", ha="center")
    y -= dy
    # 步骤 4
    ax.annotate("", xy=(6.8, y), xytext=(7.2, y), arrowprops=dict(arrowstyle="->", color="orange", lw=1.5))
    ax.text(7.5, y + 0.15, "4. 校验 JWT", fontsize=9, color="orange", ha="center")
    y -= dy
    # 步骤 5
    ax.annotate("", xy=(8.5, y), xytext=(6.8, y), arrowprops=dict(arrowstyle="->", color="blue", lw=1.5))
    ax.text(7.65, y + 0.15, "5. 返回资源", fontsize=9, color="blue", ha="center")

    ax.text(5, 0.8, "OAuth2.0 + JWT 流程示意", fontsize=12, ha="center", style="italic")
    plt.tight_layout()

    out_dir = os.path.join(os.path.dirname(__file__), "..", "assets")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "oauth2_jwt_flow_diagram_cn.png")
    plt.savefig(out_path, dpi=150, bbox_inches="tight")
    plt.close()
    print("已生成:", os.path.abspath(out_path))

if __name__ == "__main__":
    main()
