# AGENTS.md

## Git ワークフロー

### PR作成
GitHub CLI (`gh` コマンド) を使用してPRを作成すること。

```bash
# 変更をコミット
git add <files>
git commit -m "commit message"

# ブランチをpush
git push origin <branch-name>

# ghコマンドでPR作成
gh pr create --title "PRタイトル" --body "PR本文" --base main
```

**注意:** `git` コマンドだけでなく、必ず `gh` コマンドを使ってPRを作成すること。
