# Thiết kế hệ thống đa vai trò: Học sinh, Giáo viên, Phụ huynh, Admin

Ngày: 2026-07-16
Trạng thái: Đã chốt với chủ sản phẩm, chờ lập kế hoạch thực thi

## 1. Mục tiêu

Mở rộng My FSchool từ ứng dụng chỉ dành cho học sinh thành hệ thống bốn vai trò
cho trường trung học phổ thông Việt Nam (khối 10-12). Học sinh, Giáo viên và
Phụ huynh dùng chung ứng dụng Flutter với bộ màn hình riêng cho từng vai. Admin
tiếp tục là ứng dụng React Web tách biệt.

### 1.1. Quan hệ với tài liệu hiện có

Tài liệu này **thay thế** `docs/MOBILE_ROLES_ROADMAP.md`. Roadmap đó mâu thuẫn với
thiết kế đã chốt ở ba điểm: nó giao cho giáo viên chủ nhiệm xác nhận yêu cầu sửa
điểm (nay là Admin), nó không có khái niệm cột điểm, và nó mô tả Thông tư 15/2026
như căn cứ đánh giá (đánh giá thuộc Thông tư 22/2021). Roadmap cũ phải được xoá
hoặc đánh dấu đã thay thế trong R1; giữ cả hai là tạo hai nguồn sự thật về nghiệp
vụ, đúng loại lỗi mà thiết kế này tìm cách loại bỏ khỏi dữ liệu.

`docs/EDUCATION_RULES.md` và `docs/IMPLEMENTATION_ROADMAP.md` mô tả phần học sinh
đã hoàn thành và vẫn còn hiệu lực. Mục 2 của tài liệu này bổ sung căn cứ pháp lý
mới cho các vai còn lại chứ không phủ nhận chúng.

## 2. Căn cứ pháp lý

Mọi căn cứ dưới đây đã được kiểm chứng ngày 2026-07-16.

### 2.1. Điều lệ trường học

Thông tư 15/2026/TT-BGDĐT ban hành ngày 24/3/2026, có hiệu lực từ 10/5/2026,
ban hành Điều lệ trường tiểu học, trường trung học cơ sở, trường trung học phổ
thông và trường phổ thông có nhiều cấp học. Thông tư này thay thế Thông tư
32/2020/TT-BGDĐT.

Nguồn: <https://xaydungchinhsach.chinhphu.vn/toan-van-thong-tu-so-15-2026-tt-bgddt-dieu-le-truong-tieu-hoc-truong-trung-hoc-co-so-truong-trung-hoc-pho-thong-va-truong-pho-thong-co-nhieu-cap-hoc-119260504162539719.htm>

#### Bảo mật kết quả học tập là quy định bắt buộc

Khoản 2 Điều 22 Điều lệ quy định việc kiểm tra, đánh giá học sinh phải bảo đảm
tính toàn diện, công bằng, trung thực, khách quan, **không so sánh học sinh này
với học sinh khác**, bảo đảm **nguyên tắc bảo mật thông tin, kết quả học tập của
học sinh**, và không gây áp lực cho học sinh, giáo viên và cha mẹ học sinh.

Đây là ràng buộc sản phẩm, không phải khuyến nghị. Hệ thống **không được** có:

- xếp hạng học sinh trong lớp, trong khối hoặc toàn trường;
- bảng vinh danh, bảng xếp hạng, biểu đồ so sánh học sinh với bạn cùng lớp;
- điểm trung bình lớp, phổ điểm hoặc thứ hạng hiển thị cho học sinh và phụ huynh;
- bất kỳ màn hình nào để một học sinh hoặc phụ huynh nhìn thấy kết quả của học
  sinh khác.

Giáo viên bộ môn nhìn thấy bảng điểm cả lớp là hợp lệ vì đó là điều kiện để thực
hiện nhiệm vụ chuyên môn được phân công. Ràng buộc này áp cho phía học sinh và
phụ huynh. Kiểm thử phải chứng minh ràng buộc, không chỉ mô tả nó.

### 2.2. Đánh giá học sinh

Thông tư 22/2021/TT-BGDĐT vẫn là căn cứ đánh giá học sinh trung học cơ sở và
trung học phổ thông trong năm 2026.

Nguồn: <https://vanban.chinhphu.vn/?pageid=27160&docid=203926>

Các quy định được hệ thống thực thi:

**Số lượng điểm đánh giá thường xuyên mỗi học kì (Điều 6)** phụ thuộc số tiết
trong năm học của môn: từ 35 tiết trở xuống là 02 điểm; trên 35 đến 70 tiết là
03 điểm; trên 70 tiết là 04 điểm.

**Công thức tính điểm (Điều 9)**

```
ĐTBmhk = (tổng ĐĐGtx + 2 × ĐĐGgk + 3 × ĐĐGck) / (số ĐĐGtx + 5)
ĐTBmcn = (ĐTBmhkI + 2 × ĐTBmhkII) / 3
```

Hệ thống dùng số học thập phân, làm tròn một chữ số thập phân với HALF_UP, không
dùng dấu phẩy động nhị phân. Quy tắc này đã áp dụng cho ĐTBmhk và được giữ
nguyên cho ĐTBmcn.

**Xếp loại kết quả học tập (Điều 9)** theo 04 mức:

| Mức | Điều kiện |
| --- | --- |
| Tốt | Tất cả môn nhận xét ở mức Đạt; tất cả môn có điểm số có ĐTB từ 6,5 trở lên, trong đó ít nhất 06 môn có ĐTB từ 8,0 trở lên |
| Khá | Tất cả môn nhận xét ở mức Đạt; tất cả môn có điểm số có ĐTB từ 5,0 trở lên, trong đó ít nhất 06 môn có ĐTB từ 6,5 trở lên |
| Đạt | Có nhiều nhất 01 môn nhận xét ở mức Chưa đạt; có ít nhất 06 môn có điểm số với ĐTB từ 5,0 trở lên; không có môn nào có ĐTB dưới 3,5 |
| Chưa đạt | Các trường hợp còn lại |

**Quy tắc điều chỉnh:** nếu mức xếp loại bị thấp xuống từ 02 mức trở lên so với
mức lẽ ra đạt được, và nguyên nhân chỉ do kết quả của duy nhất 01 môn học, thì
mức xếp loại được điều chỉnh lên mức liền kề. Quy tắc này bắt buộc phải cài đặt.

Nguồn: <https://luatvietnam.vn/giao-duc/thong-tu-22-2021-tt-bgddt-207846-d1.html>,
<https://hoatieu.vn/phap-luat/thong-tu-22-2021-tt-bgddt-ve-danh-gia-hoc-sinh-thcs-va-thpt-210333>

**Xếp loại kết quả rèn luyện (Điều 8)** do giáo viên chủ nhiệm đánh giá, theo 04
mức Tốt, Khá, Đạt, Chưa đạt. Rèn luyện cả năm suy ra từ hai học kì:

| Mức cả năm | Điều kiện |
| --- | --- |
| Tốt | Học kì II Tốt và học kì I từ Khá trở lên |
| Khá | Học kì II Khá và học kì I từ Đạt trở lên; hoặc học kì II Đạt và học kì I Tốt; hoặc học kì II Tốt và học kì I Đạt hoặc Chưa đạt |
| Đạt | Học kì II Đạt và học kì I Khá, Đạt hoặc Chưa đạt; hoặc học kì II Khá và học kì I Chưa đạt |
| Chưa đạt | Các trường hợp còn lại |

**Lên lớp (Điều 12):** kết quả rèn luyện cả năm từ Đạt trở lên, kết quả học tập
cả năm từ Đạt trở lên, và nghỉ học không quá 45 buổi trong một năm học.

Học sinh chưa đủ điều kiện lên lớp nhưng có rèn luyện cả năm từ Đạt trở lên thì
kiểm tra lại trong kì nghỉ hè các môn có ĐTBmcn dưới 5,0 hoặc môn nhận xét ở mức
Chưa đạt. Học sinh có rèn luyện cả năm Chưa đạt thì rèn luyện trong kì nghỉ hè.

Nguồn: <https://giaoduc.net.vn/cac-truong-hop-hoc-sinh-phai-kiem-tra-lai-o-lai-lop-theo-thong-tu-22-cua-bo-gd-post242024.gd>

**Khen thưởng (Điều 15):** danh hiệu Học sinh Xuất sắc yêu cầu rèn luyện Tốt,
học tập Tốt và ít nhất 06 môn có ĐTBmcn từ 9,0 trở lên. Danh hiệu Học sinh Giỏi
yêu cầu rèn luyện Tốt và học tập Tốt.

### 2.3. Bảo vệ dữ liệu cá nhân

Luật Bảo vệ dữ liệu cá nhân số 91/2025/QH15 có hiệu lực từ 01/01/2026. Nghị định
356/2025/NĐ-CP ngày 31/12/2025 quy định chi tiết thi hành, có hiệu lực cùng ngày
và **thay thế Nghị định 13/2023/NĐ-CP**.

Nguồn: <https://vanban.chinhphu.vn/?docid=216387&pageid=27160>,
<https://luatvietnam.vn/thong-tin/nghi-dinh-356-2025-nd-cp-quy-dinh-chi-tiet-luat-bao-ve-du-lieu-ca-nhan-422896-d1.html>

Hệ thống xử lý dữ liệu học sinh trung học phổ thông, trong đó có người dưới 16
tuổi. Thiết kế này áp dụng các biện pháp kỹ thuật đã biết là cần thiết trong mọi
trường hợp: giảm thiểu dữ liệu, phân quyền theo quan hệ, nhật ký truy vết mọi
thay đổi, và không lộ dữ liệu xuyên chủ thể.

**Chưa xác minh trong đợt thiết kế này:** các mốc tuổi cụ thể để xác định ai có
quyền đồng ý xử lý dữ liệu, và ranh giới giữa quyền của học sinh đã đủ 16 tuổi
với quyền của người giám hộ. Spec này **không đưa ra khẳng định** về các mốc đó.
Trước khi vận hành thật, phần dữ liệu người chưa thành niên cần được rà soát bởi
người có chuyên môn pháp lý. Đây là việc phải làm, được ghi nhận ở mục 12, không
phải việc kỹ thuật có thể tự quyết.

### 2.4. Giới hạn đã biết của căn cứ pháp lý

Điều kiện "nghỉ học không quá 45 buổi" không thể tính tự động vì hệ thống không
có chức năng điểm danh (xem mục 11). Giáo viên chủ nhiệm nhập số buổi nghỉ khi
xét lên lớp. Đây là đánh đổi đã được chấp nhận, không phải thiếu sót.

Văn bản quy phạm trong lĩnh vực giáo dục thay đổi rất nhanh trong giai đoạn
2024-2026. Mọi căn cứ trong mục 2 đều ghi ngày kiểm chứng và văn bản thay thế nếu
có. Khi bổ sung căn cứ mới, phải kiểm tra **hiệu lực** chứ không chỉ nội dung:
quy chế đăng trên trang của trường và tài liệu của các phần mềm đang lưu hành có
thể vẫn dẫn văn bản đã bị bãi bỏ.

## 3. Nguyên tắc phân quyền

Đây là nguyên tắc quan trọng nhất của thiết kế.

**Quyền không đến từ vai trò, quyền đến từ quan hệ.**

Một tài khoản có vai `TEACHER` tự nó không cho phép xem bất kỳ dữ liệu nào. Giáo
viên chỉ thao tác được trên tổ hợp lớp-môn-học kì mà họ có bản ghi phân công đang
hiệu lực. Một tài khoản có vai `PARENT` chỉ xem được học sinh mà họ có liên kết
giám hộ đang hiệu lực.

Hệ quả bắt buộc:

- Server không bao giờ tin `classId`, `subjectId`, `studentId` hay `childId` do
  client gửi lên. Mọi định danh nhận từ client đều phải đối chiếu lại với bản ghi
  phân công hoặc liên kết giám hộ trước khi dùng.
- Vai trò được phân giải từ principal đã xác thực, giống hệt cách
  `StudentProfileStore.findByUserId()` đang làm cho học sinh.
- Không dùng chuỗi tên người để phân quyền. Đây là lý do `teacher_name` phải trở
  thành khoá ngoại (mục 5.2).
- Tầng security giữ nguyên `denyAll()` mặc định.

## 4. Vai trò và nhiệm vụ

| Năng lực | Học sinh | Giáo viên | Phụ huynh |
| --- | --- | --- | --- |
| Hồ sơ | Của mình | Hồ sơ nhân sự của mình | Chỉ con đã liên kết |
| Thời khoá biểu | Lớp mình | Lịch dạy của mình | Của con đang chọn |
| Điểm | Điểm đã công bố của mình | Nhập điểm lớp-môn được phân công | Điểm đã công bố của con |
| Đơn từ | Tạo và theo dõi đơn của mình | GVCN duyệt đơn nghỉ của lớp chủ nhiệm | Tạo cho con và theo dõi |
| Sự kiện, câu lạc bộ | Xem và đăng ký | Xem hoạt động trường | Xem trạng thái của con |
| Thông báo | Cá nhân | Theo lớp dạy và lớp chủ nhiệm | Theo con đang chọn |
| Trợ lý AI | Chỉ đọc dữ liệu của mình | Chỉ đọc dữ liệu được phân công | Chỉ đọc dữ liệu con đang chọn |
| Tổng kết | Xem kết quả đã công bố | GVCN nhập rèn luyện và chốt | Xem kết quả đã công bố của con |

### 4.1. Giáo viên bộ môn

Xem lịch dạy sinh từ phân công đang hiệu lực. Xem danh sách học sinh chỉ của tổ
hợp lớp-môn-học kì được phân công. Tạo cột điểm và nhập điểm khi sổ điểm còn mở.
Công bố điểm cho học sinh và phụ huynh. Khi sổ điểm đã khoá, gửi yêu cầu sửa điểm
thay vì ghi đè trực tiếp. Mọi thay đổi điểm ghi lại người thực hiện, giá trị cũ,
giá trị mới, lý do và thời điểm.

### 4.2. Giáo viên chủ nhiệm

Mỗi lớp có một giáo viên chủ nhiệm theo năm học. Một giáo viên có thể vừa chủ
nhiệm một lớp vừa dạy bộ môn ở các lớp khác; hai quan hệ này độc lập. Giáo viên
không chủ nhiệm lớp nào thì giao diện không hiển thị mục chủ nhiệm.

Giáo viên chủ nhiệm xem toàn bộ danh sách lớp chủ nhiệm và bảng tổng hợp học tập.
Duyệt đơn xin nghỉ học của học sinh lớp mình. Nhập xếp loại rèn luyện và chốt
tổng kết. Giáo viên chủ nhiệm không sửa được điểm môn của giáo viên khác và không
duyệt yêu cầu sửa điểm.

### 4.3. Phụ huynh

Một phụ huynh liên kết được với nhiều học sinh; một học sinh có nhiều người giám
hộ. Liên kết có loại quan hệ, khoảng hiệu lực và thứ tự liên hệ. Phụ huynh chọn
một con, và mọi truy vấn sau đó đều được server kiểm tra lại liên kết đó.

Phụ huynh xem thời khoá biểu, điểm đã công bố, thông báo, đơn từ, trạng thái sự
kiện và câu lạc bộ của con. Tạo và theo dõi đơn cho con. Xác nhận thông báo và
yêu cầu đồng thuận. Phụ huynh không sửa điểm, không đăng ký hoạt động thay con,
không xem dữ liệu học sinh khác.

### 4.4. Admin

Admin tạo và quản lý toàn bộ dữ liệu gốc: hồ sơ giáo viên, hồ sơ phụ huynh, liên
kết phụ huynh - học sinh, phân công giảng dạy, phân công chủ nhiệm. Admin khoá và
mở sổ điểm, duyệt yêu cầu sửa điểm, và duyệt các đơn hành chính.

## 5. Mô hình dữ liệu

### 5.1. Danh tính và quan hệ

| Bảng | Nội dung |
| --- | --- |
| `user_roles` | Một tài khoản có nhiều vai. Thay thế cột `users.role` |
| `teacher_profiles` | Hồ sơ giáo viên. `user_id` cho phép NULL |
| `parent_profiles` | Hồ sơ phụ huynh |
| `parent_student_links` | Liên kết phụ huynh - học sinh: quan hệ, khoảng hiệu lực, thứ tự liên hệ |

Cột `users.role` bị xoá sau khi chuyển dữ liệu sang `user_roles`. Giữ cả hai tạo
ra hai nguồn sự thật về vai trò, và đó là nguồn sinh lỗi phân quyền.

`teacher_profiles.user_id` cho phép NULL vì nhà trường nhập danh sách giáo viên
trước, cấp tài khoản sau. Hồ sơ giáo viên chưa có tài khoản vẫn được phân công và
gắn vào thời khoá biểu; chỉ là chưa đăng nhập được.

### 5.2. Phân công

| Bảng | Khoá duy nhất |
| --- | --- |
| `teacher_subject_assignments` | (lớp, môn, học kì) — mỗi tổ hợp có đúng một giáo viên chịu trách nhiệm |
| `homeroom_assignments` | (lớp, năm học) — mỗi lớp có đúng một giáo viên chủ nhiệm |

### 5.3. Sửa khiếm khuyết dữ liệu hiện có

Hai lỗi hiện có cản trở trực tiếp việc phân quyền và phải được sửa:

**`class_timetable_entries.teacher_name`** đang là chuỗi tự do không khoá ngoại.
Không thể dùng chuỗi tên để phân quyền vì hai giáo viên trùng tên sẽ nhìn thấy
dữ liệu của nhau. Migration gom các tên đang có trong thời khoá biểu và
`school_clubs.advisor_name`, tạo `teacher_profiles` tương ứng với `user_id` NULL,
gắn `teacher_id`, đặt NOT NULL, rồi xoá cột cũ. Rủi ro đã biết: hai giáo viên
trùng tên trong dữ liệu cũ bị gộp làm một; Admin tách tay sau migration.

**`students.class_name` và `students.class_id`** tồn tại song song từ V14. Danh
sách lớp của giáo viên và quan hệ chủ nhiệm dựa vào `class_id`, nên `class_name`
bị xoá để loại bỏ nguồn sự thật thứ hai.

### 5.4. Sổ điểm

| Bảng | Nội dung |
| --- | --- |
| `grade_books` | Theo (lớp, môn, học kì). Trạng thái mở/khoá, thời điểm công bố |
| `grade_columns` | Cột điểm của sổ: loại, hình thức, nhãn, thứ tự |
| `grade_change_requests` | Yêu cầu sửa điểm sau khi khoá sổ |

`grade_assessments` thêm khoá ngoại tới `grade_columns`. Cột điểm là thực thể nên
bảng điểm của lớp luôn thẳng hàng theo cấu tạo, không phụ thuộc vào việc nhập
liệu có nhất quán hay không. Đổi nhãn cột sửa một bản ghi.

`grade_assessments.status` thêm giá trị `PENDING` (chưa nhập điểm). Ràng buộc
`ck_grade_assessments_result_by_status` hiện bắt buộc trạng thái `RECORDED` phải
có điểm, nên không biểu diễn được ô trống. Giáo viên tạo cột rồi nhập dần là
hành vi bình thường và phải được mô hình hoá.

Migration dữ liệu cũ: với mỗi sổ điểm, sinh cột từ các bộ (thứ tự, loại, hình
thức, nhãn) đang có; gán mỗi đầu điểm vào cột khớp; đầu điểm không khớp cột nào
thì sinh cột mới cho nó. Không mất dữ liệu.

### 5.5. Công bố và khoá sổ

Đây là hai khái niệm khác nhau và không được gộp:

- **Công bố**: học sinh và phụ huynh nhìn thấy điểm. Do giáo viên bộ môn thực
  hiện theo từng sổ điểm.
- **Khoá sổ**: giáo viên hết quyền sửa trực tiếp. Do Admin thực hiện.

Sổ đã công bố nhưng chưa khoá thì giáo viên vẫn sửa được, mọi sửa đổi ghi nhật
ký. Sổ đã khoá thì phải qua `grade_change_requests` và Admin duyệt.

Toàn bộ điểm đang có trong hệ thống được migration đánh dấu đã công bố, để hành
vi của ứng dụng học sinh và các bộ kiểm thử đầu-cuối hiện tại không đổi.

### 5.6. Tổng kết

| Bảng | Nội dung |
| --- | --- |
| `student_term_summaries` | Theo học kì: xếp loại rèn luyện, xếp loại học tập, nhận xét, thời điểm chốt, thời điểm công bố |
| `student_year_summaries` | Theo năm: xếp loại cả năm, số buổi nghỉ do GVCN nhập, kết quả lên lớp, danh hiệu |

Xếp loại học tập được tính tự động theo mục 2.2. Xếp loại rèn luyện do giáo viên
chủ nhiệm nhập. Kết quả lên lớp suy ra từ hai xếp loại cả năm và số buổi nghỉ.

### 5.7. Nhật ký

Bảng `admin_audit_events` được đổi tên thành `data_audit_events` và thêm cột vai
trò của người thực hiện. Thay đổi điểm do giáo viên không phải hành vi admin, và
hệ thống chỉ nên có một dấu vết thay đổi dữ liệu. Cấu trúc hiện tại (người thực
hiện, hành động, loại thực thể, định danh thực thể, các trường thay đổi dạng
JSONB, thời điểm) đã đủ tổng quát.

Bảng `security_audit_events` giữ nguyên, phục vụ sự kiện bảo mật.

`student_form_status_history` hiện **không lưu ai đổi trạng thái**. Đây là lỗi
phải sửa: duyệt đơn là hành vi có trách nhiệm và bắt buộc phải có dấu vết.

## 6. Không gian tên API

```
/api/v1/home, /timetable, /grades, /events, /forms, /clubs, /assistant
                      giữ nguyên — vai học sinh
/api/v1/teacher/**    mới
/api/v1/parent/**     mới
/api/v1/admin/**      giữ nguyên
```

**Quyết định ngày 2026-07-16 (thay thế phương án chuẩn hoá trước đó):** đường dẫn
của vai học sinh **giữ nguyên**, không chuyển sang `/api/v1/student/**`. Việc
chuyển là thay đổi phá vỡ với ứng dụng Flutter và 11 bộ kiểm thử đầu-cuối đang
xanh, đổi lại chỉ được sự cân xứng về tên gọi. Cái giá đó không tương xứng với
lợi ích.

Hệ quả phải chấp nhận: không gian tên không cân xứng — hai vai mới có tiền tố
riêng, vai học sinh thì không. Đây là nợ hình thức, không phải nợ phân quyền:
tầng security vẫn liệt kê tường minh từng đường dẫn học sinh và vẫn `denyAll()`
mặc định, nên ranh giới giữa các vai không lỏng đi vì quyết định này.

## 7. Luồng nghiệp vụ chính

### 7.1. Sổ điểm

```
Admin phân công GV Toán dạy 10A1 học kì I
        ↓
GV mở sổ điểm 10A1 - Toán - HKI (trạng thái Mở, chưa công bố)
        ↓
GV tạo cột "Miệng 1" → hệ sinh ô PENDING cho mọi HS trong lớp
        ↓
GV nhập điểm → RECORDED
        ↓
GV bấm Công bố → HS và PH thấy điểm
        ↓
Admin khoá sổ cuối kì → GV hết quyền sửa trực tiếp
        ↓
GV phát hiện sai → gửi yêu cầu sửa (cũ, mới, lý do)
        ↓
Admin duyệt → điểm đổi, nhật ký ghi đầy đủ, ĐTB tính lại
```

### 7.2. Đơn từ

Đơn xin nghỉ học do học sinh hoặc phụ huynh tạo, và **giáo viên chủ nhiệm của
lớp học sinh đó** duyệt. Các đơn hành chính (xác nhận học sinh, cấp bảng điểm,
cấp lại thẻ) do học sinh hoặc phụ huynh tạo và Admin duyệt.

`student_forms` thêm người nộp và vai của người nộp. Mọi lần đổi trạng thái ghi
người thực hiện, thời điểm và lý do.

### 7.3. Tổng kết học kì

```
GV bộ môn nhập đủ đầu điểm → Admin khoá sổ điểm học kì
        ↓
Hệ thống tính ĐTBmhk từng môn
        ↓
GVCN nhập xếp loại rèn luyện học kì
        ↓
Hệ thống tính xếp loại kết quả học tập (mục 2.2), áp dụng quy tắc
điều chỉnh lên mức liền kề
        ↓
GVCN chốt và công bố → HS và PH xem kết quả
```

Cuối năm, hệ thống tính ĐTBmcn, xếp loại cả năm, rèn luyện cả năm; GVCN nhập số
buổi nghỉ; hệ thống suy ra kết quả lên lớp và danh hiệu.

## 8. Điều hướng Flutter

### 8.1. Cấu trúc

Ba vai có shell, bộ đường dẫn và repository riêng, dùng chung theme, phiên đăng
nhập, tầng mạng, xử lý lỗi và trợ lý AI.

```
lib/src/features/       dùng chung: auth, notifications, assistant
lib/src/roles/student/  shell + 5 tab hiện có, giữ nguyên
lib/src/roles/teacher/  shell + màn giáo viên
lib/src/roles/parent/   shell + màn phụ huynh
```

Tách theo thư mục để một màn hình không thể gọi nhầm API của vai khác. Ranh giới
này được cấu trúc bảo đảm, không dựa vào kỷ luật lập trình.

### 8.2. Học sinh: giữ nguyên, không thêm đường dẫn mới

Năm tab hiện có (Trang chủ, Lịch học, Kết quả, Hoạt động, Cá nhân) và các màn con
đã có giữ nguyên. Xếp loại học kì hiển thị **bên trong** tab Kết quả. Không phát
sinh màn hình mới cho vai học sinh.

### 8.3. Giáo viên

| Tab | Nội dung |
| --- | --- |
| Tổng quan | Tiết dạy hôm nay, việc cần làm, thông báo |
| Lịch dạy | Thời khoá biểu dạy theo tuần |
| Lớp | Lớp-môn được phân công; lớp chủ nhiệm gồm duyệt đơn và tổng kết |
| Sổ điểm | Chọn lớp-môn-học kì, nhập điểm, công bố |
| Cá nhân | Hồ sơ, đổi vai, đăng xuất |

Mục lớp chủ nhiệm chỉ hiển thị khi tài khoản có phân công chủ nhiệm đang hiệu lực.

### 8.4. Phụ huynh

| Tab | Nội dung |
| --- | --- |
| Tổng quan | Con đang chọn, lịch học hôm nay, thông báo cần xác nhận |
| Lịch học | Thời khoá biểu của con |
| Kết quả | Điểm đã công bố và tổng kết của con |
| Đơn từ | Tạo đơn cho con, theo dõi, xác nhận |
| Cá nhân | Chọn con, hồ sơ, đổi vai, đăng xuất |

### 8.5. Đăng nhập nhiều vai

Đăng nhập trả về danh sách vai khả dụng. Tài khoản một vai vào thẳng. Tài khoản
nhiều vai (giáo viên đồng thời là phụ huynh) chọn ngữ cảnh. Access token và
refresh token gắn với vai đang hoạt động. Đổi vai tạo phiên mới gắn vai mới.

## 9. Admin Web

Bổ sung sáu trang: Giáo viên, Phân công giảng dạy, Phân công chủ nhiệm, Phụ
huynh, Khoá/mở sổ điểm, Duyệt yêu cầu sửa điểm. Trang Lịch học sửa để gắn giáo
viên thật thay cho tên tự do.

**Quyết định ngày 2026-07-17 (thay thế phương án bảy trang trước đó):** liên kết
phụ huynh - học sinh **không tách thành trang riêng** mà nằm trong trang Phụ
huynh. Một liên kết luôn thuộc về một phụ huynh cụ thể, nên thao tác tự nhiên là
mở phụ huynh đó ra rồi thêm hoặc gỡ con. Trang riêng buộc người dùng nhớ tên cả
hai phía rồi ghép lại, và tạo thêm một chỗ nữa để đọc cùng một dữ liệu.

Admin không có ứng dụng di động. Phiên Admin giữ nguyên cơ chế cookie httpOnly
kèm CSRF, tách biệt khỏi luồng JWT của di động.

## 10. Kiểm thử

Mỗi phase phải có: migration Flyway, kiểm thử tích hợp Spring chạy trên
PostgreSQL thật qua Testcontainers, kiểm thử đơn vị và widget Flutter, **kiểm thử
phân quyền phủ định**, và một bộ Playwright riêng. Không phase nào hoàn thành khi
một bộ hồi quy Học sinh hoặc Admin trước đó còn đỏ.

Kiểm thử phân quyền phủ định là bắt buộc, không phải tuỳ chọn:

- Giáo viên không được phân công truy cập sổ điểm lớp khác nhận 403.
- Phụ huynh truy vấn học sinh không có liên kết nhận 403.
- Học sinh và phụ huynh không thấy điểm chưa công bố.
- Giáo viên chủ nhiệm lớp khác không duyệt được đơn.
- Vai này không gọi được API của vai kia.
- Không endpoint nào của vai học sinh hoặc phụ huynh trả về kết quả học tập của
  một học sinh khác, thứ hạng, hay điểm trung bình lớp (mục 2.1).

## 11. Ngoài phạm vi

- **Điểm danh và chuyên cần.** Chỉ có đơn xin nghỉ học. Hệ quả: hệ thống không
  tự tính được số buổi nghỉ và không cảnh báo học sinh nghỉ quá số buổi cho phép;
  giáo viên chủ nhiệm nhập tay số buổi nghỉ khi xét lên lớp.
- Thanh toán học phí trực tuyến và đối soát kế toán.
- Chat tự do giữa phụ huynh và giáo viên. Thay bằng đơn từ và xác nhận có dấu vết.
- Lương, nghỉ phép giáo viên và các quy trình nhân sự.
- Nộp bài tập trực tuyến và thi trực tuyến.
- **Ký số sổ điểm và học bạ điện tử.** Sổ điểm được chốt bằng khoá sổ và nhật ký
  truy vết, không bằng chữ ký số. Học bạ điện tử có chuỗi ký riêng và cần yêu cầu
  riêng, không đưa vào đợt này.
- **Xếp hạng, vinh danh, so sánh học sinh.** Không phải "chưa làm" mà là **không
  được làm**, theo khoản 2 Điều 22 Thông tư 15/2026 (mục 2.1).
- **Hệ số môn học và cột "điểm miệng" riêng.** Thông tư 22/2021 không có hệ số
  môn; hệ số môn là di sản của Thông tư 58/2011 đã hết hiệu lực. "Miệng" chỉ là
  hình thức đánh giá (`assessment_form`), không phải một loại điểm có hệ số riêng.
  Hệ quả cần lường trước: nếu đối chiếu số với phần mềm khác mà trường đó còn đặt
  hệ số môn khác 1, kết quả sẽ lệch — và hệ thống này đúng theo Thông tư 22.

Các mảng này cần yêu cầu riêng về tài chính, kiểm duyệt, lưu trữ hoặc liêm chính
học thuật, và không nên giấu bên trong đợt phát hành ba vai đầu tiên.

## 12. Rủi ro và đánh đổi đã chấp nhận

| Rủi ro | Xử lý |
| --- | --- |
| Gộp nhầm giáo viên trùng tên khi migrate `teacher_name` | Admin tách tay sau migration |
| Thêm cổng công bố làm học sinh không thấy điểm cũ | Migration đánh dấu toàn bộ điểm hiện có là đã công bố |
| Không có điểm danh nên thiếu một điều kiện lên lớp của Thông tư 22 | GVCN nhập tay số buổi nghỉ |
| Mốc tuổi đồng ý xử lý dữ liệu của người chưa thành niên chưa được xác minh | Rà soát pháp lý bắt buộc trước khi vận hành thật (mục 2.3). Spec không khẳng định mốc tuổi |
| Nhập điểm hàng loạt từ tệp có thể trở thành đường tắt bỏ qua vòng duyệt | Nếu về sau thêm chức năng nhập từ tệp, nó phải đi qua đúng cơ chế khoá sổ, duyệt và nhật ký như nhập tay. Không có ngoại lệ |

## 13. Các phase

| Phase | Phạm vi | Cổng nghiệm thu |
| --- | --- | --- |
| R1 | `user_roles`, hồ sơ giáo viên và phụ huynh, liên kết giám hộ, JWT gắn vai, đổi vai, Admin quản lý giáo viên/phụ huynh/liên kết | Giáo viên và phụ huynh không gọi được API của nhau; hồi quy học sinh xanh |
| R2 | Phân công giảng dạy và chủ nhiệm, `teacher_name` thành khoá ngoại, dọn `class_name`, lịch dạy, danh sách lớp | Giáo viên chỉ thấy tiết dạy và học sinh thuộc phân công của mình |
| R3 | Sổ điểm, cột điểm, trạng thái `PENDING`, công bố, khoá sổ, yêu cầu sửa điểm, nhật ký | Giáo viên không được phân công nhận 403; điểm chưa công bố không lộ |
| R4 | API phụ huynh, chọn con, thời khoá biểu và điểm của con | Phụ huynh không truy vấn được học sinh không liên kết |
| R5 | Đơn từ đa vai, định tuyến duyệt theo GVCN, dấu vết người duyệt, thông báo và xác nhận | Phụ huynh nộp đơn cho con và GVCN xử lý trọn vòng; GVCN lớp khác nhận 403 |
| R6 | Tổng kết học kì và cả năm, xếp loại, lên lớp, danh hiệu, AI theo vai, siết bảo mật | AI chỉ dùng dữ liệu trong phạm vi vai đang hoạt động; E2E đủ bốn vai |
