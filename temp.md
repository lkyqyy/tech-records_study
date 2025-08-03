✅ 步骤一：查看当前修改状态
bash
复制
编辑
git status
你会看到如下三类内容：

modified：已修改文件

deleted：被删除的文件

untracked：新添加但未纳入版本控制的文件

✅ 步骤二：添加所有改动
推荐使用下面命令统一添加所有变动（包括新增、修改、删除）：

bash
复制
编辑
git add -A
或者更明确一点：

bash
复制
编辑
git add .
✅ 步骤三：提交更改
使用提交命令写一条说明清晰的提交信息，例如：

bash
复制
编辑
git commit -m "优化 PyQt5 图像交互脚本，修复缩放坐标不准问题"
✅ 步骤四：拉取最新远程代码（避免冲突）
建议使用 pull --rebase 保持历史更整洁：

bash
复制
编辑
git pull --rebase origin main
如遇冲突按提示解决后：

bash
复制
编辑
git add <冲突文件>
git rebase --continue
✅ 步骤五：推送到远程
bash
复制
编辑
git push origin main
💡 示例流程汇总：
bash
复制
编辑
git status
git add -A
git commit -m "更新 PyQt5 框选区域逻辑，修复缩放偏差"
git pull --rebase origin main
git push origin main
如果你仓库是多分支协作，还可以加上 git switch <branch> 或创建新分支操作。需要我为你整理一个 .gitignore、或做一个图像项目 .git 使用模板目录也可以继续告诉我。