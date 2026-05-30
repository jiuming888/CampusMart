@echo off
chcp 65001 >nul
echo ========================================
echo   CampusMart 校园闲置交易平台
echo ========================================
echo.

:: 检查前端依赖
if not exist "node_modules" (
    echo [1/3] 安装前端依赖...
    call npm install
) else (
    echo [1/3] 前端依赖已安装，跳过...
)

echo.
echo ========================================
echo  启动完成！
echo ========================================
echo.
echo 前端开发服务器: http://localhost:5173
echo 后端API: http://localhost:8080
echo.
echo 提示：
echo   1. 确保 MySQL 数据库已启动并导入数据
echo   2. 后端需要单独启动 (cd backend && mvn spring-boot:run)
echo   3. 管理员账号: admin / admin123
echo   4. 测试用户: testuser / admin123
echo.
echo 按任意键启动前端开发服务器...
pause >nul

:: 启动前端
npm run dev
