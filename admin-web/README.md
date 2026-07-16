# FSchool Admin Web

Ứng dụng quản trị độc lập cho My FSchool, dùng chung Spring Boot API và PostgreSQL với ứng dụng Flutter của học sinh.

## Yêu cầu

- Node.js 22+
- Spring Boot API đang chạy tại `http://127.0.0.1:8080`

## Chạy local

```powershell
npm install
npm run dev
```

Mở `http://127.0.0.1:4174`. Vite chuyển tiếp `/api` đến backend nên cookie quản trị luôn cùng origin trong môi trường phát triển.

Tài khoản ADMIN của profile `dev` và `e2e`:

- Số điện thoại: `0900000000`
- Mật khẩu: `Admin@123`

Seed ADMIN không được kích hoạt trong production.

## Kiểm tra

```powershell
npm run lint
npm run test
npm run build
npm run test:e2e
```

Playwright cần backend và PostgreSQL đang chạy. Access token chỉ tồn tại trong bộ nhớ trình duyệt; refresh token nằm trong cookie HttpOnly, SameSite=Strict và refresh/logout bắt buộc có CSRF token.
