import AddRoundedIcon from '@mui/icons-material/AddRounded';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import FamilyRestroomOutlinedIcon from '@mui/icons-material/FamilyRestroomOutlined';
import LinkOffRoundedIcon from '@mui/icons-material/LinkOffRounded';
import LinkRoundedIcon from '@mui/icons-material/LinkRounded';
import SearchRoundedIcon from '@mui/icons-material/SearchRounded';
import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Divider,
  Drawer,
  FormControl,
  IconButton,
  InputAdornment,
  InputLabel,
  MenuItem,
  Select,
  Skeleton,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useDeferredValue, useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { z } from 'zod';
import { getStudents } from '../api/adminAcademicsApi';
import {
  createParent,
  getGuardianLinks,
  getParents,
  linkGuardian,
  unlinkGuardian,
  updateParent,
  type AdminParent,
  type Relationship,
} from '../api/adminIdentityApi';
import { ApiProblem } from '../api/adminAuthApi';
import { useAuth } from '../auth/authState';

const RELATIONSHIP_LABELS: Record<Relationship, string> = {
  FATHER: 'Bố',
  MOTHER: 'Mẹ',
  GUARDIAN: 'Người giám hộ',
};

/**
 * A phone number is optional, and asking for one turns on the password field.
 * Recording who a child's guardians are comes first; deciding which of them sign in comes later.
 */
const parentSchema = z
  .object({
    fullName: z.string().trim().min(2, 'Nhập họ tên phụ huynh').max(120),
    email: z.union([z.literal(''), z.string().trim().email('Email không hợp lệ').max(190)]),
    phoneNumber: z.union([
      z.literal(''),
      z.string().trim().regex(/^0[35789][0-9]{8}$/, 'Số điện thoại Việt Nam không hợp lệ'),
    ]),
    initialPassword: z.string().max(72),
    enabled: z.boolean(),
  })
  .refine((values) => values.phoneNumber === '' || values.initialPassword.length >= 8, {
    path: ['initialPassword'],
    message: 'Cấp tài khoản thì phải đặt mật khẩu ban đầu',
  });

type ParentFormValues = z.infer<typeof parentSchema>;

const defaultValues: ParentFormValues = {
  fullName: '',
  email: '',
  phoneNumber: '',
  initialPassword: '',
  enabled: true,
};

function errorMessage(error: unknown, fallback: string): string {
  return error instanceof ApiProblem ? error.message : fallback;
}

export function ParentsPage() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();
  const token = accessToken ?? '';
  const [search, setSearch] = useState('');
  const deferredSearch = useDeferredValue(search);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [editing, setEditing] = useState<AdminParent | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [linksFor, setLinksFor] = useState<AdminParent | null>(null);

  const filters = { page, size, query: deferredSearch.trim() || undefined };

  const parents = useQuery({
    queryKey: ['admin', 'parents', filters],
    queryFn: () => getParents(token, filters),
    enabled: Boolean(accessToken),
  });

  const form = useForm<ParentFormValues>({
    resolver: zodResolver(parentSchema),
    defaultValues,
  });

  useEffect(() => {
    setPage(0);
  }, [deferredSearch]);

  const save = useMutation({
    mutationFn: async (values: ParentFormValues) => {
      const email = values.email.trim() || undefined;
      if (editing) {
        return updateParent(token, editing.id, {
          fullName: values.fullName,
          email,
          enabled: values.enabled,
          version: editing.version,
        });
      }
      const phoneNumber = values.phoneNumber.trim() || undefined;
      return createParent(token, {
        fullName: values.fullName,
        email,
        phoneNumber,
        initialPassword: phoneNumber ? values.initialPassword : undefined,
      });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'parents'] });
      setDrawerOpen(false);
      setEditing(null);
    },
  });

  function openCreate() {
    setEditing(null);
    form.reset(defaultValues);
    save.reset();
    setDrawerOpen(true);
  }

  function openEdit(parent: AdminParent) {
    setEditing(parent);
    form.reset({
      fullName: parent.fullName,
      email: parent.email ?? '',
      phoneNumber: parent.phoneNumber ?? '',
      initialPassword: '',
      enabled: parent.enabled,
    });
    save.reset();
    setDrawerOpen(true);
  }

  const items = parents.data?.items ?? [];
  const wantsAccount = form.watch('phoneNumber').trim() !== '';

  return (
    <Stack spacing={3}>
      <Stack direction="row" spacing={2} sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
        <Box>
          <Typography variant="h5" component="h1">
            Phụ huynh
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Hồ sơ phụ huynh và liên kết với học sinh. Phụ huynh chỉ xem được học sinh đã liên kết.
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<AddRoundedIcon />} onClick={openCreate}>
          Thêm phụ huynh
        </Button>
      </Stack>

      <Card variant="outlined">
        <Box sx={{ p: 2 }}>
          <TextField
            label="Tìm theo tên hoặc số điện thoại"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            fullWidth
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchRoundedIcon fontSize="small" />
                  </InputAdornment>
                ),
              },
            }}
          />
        </Box>
        <Divider />

        {parents.isError ? (
          <Alert severity="error" sx={{ m: 2 }}>
            Không tải được danh sách phụ huynh.
          </Alert>
        ) : null}

        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Họ tên</TableCell>
                <TableCell>Số điện thoại</TableCell>
                <TableCell>Email</TableCell>
                <TableCell align="center">Số con</TableCell>
                <TableCell>Tài khoản</TableCell>
                <TableCell>Trạng thái</TableCell>
                <TableCell align="right">Thao tác</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {parents.isPending
                ? Array.from({ length: 3 }).map((_, index) => (
                    <TableRow key={index}>
                      <TableCell colSpan={7}>
                        <Skeleton height={28} />
                      </TableCell>
                    </TableRow>
                  ))
                : null}

              {!parents.isPending && items.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7}>
                    <Stack spacing={1} sx={{ alignItems: 'center', py: 4 }}>
                      <FamilyRestroomOutlinedIcon color="disabled" />
                      <Typography variant="body2" color="text.secondary">
                        Chưa có phụ huynh nào khớp bộ lọc.
                      </Typography>
                    </Stack>
                  </TableCell>
                </TableRow>
              ) : null}

              {items.map((parent) => (
                <TableRow key={parent.id} hover>
                  <TableCell>{parent.fullName}</TableCell>
                  <TableCell>{parent.phoneNumber ?? '—'}</TableCell>
                  <TableCell>{parent.email ?? '—'}</TableCell>
                  <TableCell align="center">{parent.linkedStudents}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={parent.hasAccount ? 'Đã có' : 'Chưa có'}
                      color={parent.hasAccount ? 'success' : 'default'}
                      variant={parent.hasAccount ? 'filled' : 'outlined'}
                    />
                  </TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={parent.enabled ? 'Hoạt động' : 'Ngừng'}
                      color={parent.enabled ? 'primary' : 'default'}
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title="Liên kết con">
                      <IconButton
                        aria-label={`Liên kết con của ${parent.fullName}`}
                        onClick={() => setLinksFor(parent)}
                      >
                        <LinkRoundedIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <IconButton aria-label={`Sửa ${parent.fullName}`} onClick={() => openEdit(parent)}>
                      <EditOutlinedIcon fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>

        <TablePagination
          component="div"
          count={parents.data?.totalElements ?? 0}
          page={page}
          rowsPerPage={size}
          onPageChange={(_, nextPage) => setPage(nextPage)}
          onRowsPerPageChange={(event) => {
            setSize(Number(event.target.value));
            setPage(0);
          }}
          rowsPerPageOptions={[10, 20, 50]}
          labelRowsPerPage="Số dòng"
        />
      </Card>

      <Drawer anchor="right" open={drawerOpen} onClose={() => setDrawerOpen(false)}>
        <Box
          component="form"
          onSubmit={form.handleSubmit((values) => save.mutate(values))}
          sx={{ width: { xs: '100vw', sm: 420 }, p: 3 }}
        >
          <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
            <Typography variant="h6">{editing ? 'Sửa hồ sơ phụ huynh' : 'Thêm phụ huynh'}</Typography>
            <IconButton aria-label="Đóng" onClick={() => setDrawerOpen(false)}>
              <CloseRoundedIcon />
            </IconButton>
          </Stack>

          <Stack spacing={2} sx={{ mt: 2 }}>
            {save.isError ? (
              <Alert severity="error">{errorMessage(save.error, 'Không thể lưu hồ sơ phụ huynh.')}</Alert>
            ) : null}

            <Controller
              name="fullName"
              control={form.control}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="Họ tên"
                  error={Boolean(fieldState.error)}
                  helperText={fieldState.error?.message}
                  fullWidth
                />
              )}
            />
            <Controller
              name="email"
              control={form.control}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="Email (không bắt buộc)"
                  error={Boolean(fieldState.error)}
                  helperText={fieldState.error?.message}
                  fullWidth
                />
              )}
            />

            {editing ? (
              <Controller
                name="enabled"
                control={form.control}
                render={({ field }) => (
                  <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
                    <Typography variant="body2">Hoạt động</Typography>
                    <Switch
                      checked={field.value}
                      onChange={(event) => field.onChange(event.target.checked)}
                    />
                  </Stack>
                )}
              />
            ) : (
              <>
                <Controller
                  name="phoneNumber"
                  control={form.control}
                  render={({ field, fieldState }) => (
                    <TextField
                      {...field}
                      label="Số điện thoại (để trống nếu chưa cấp tài khoản)"
                      error={Boolean(fieldState.error)}
                      helperText={fieldState.error?.message}
                      fullWidth
                    />
                  )}
                />
                {wantsAccount ? (
                  <Controller
                    name="initialPassword"
                    control={form.control}
                    render={({ field, fieldState }) => (
                      <TextField
                        {...field}
                        label="Mật khẩu ban đầu"
                        type="password"
                        error={Boolean(fieldState.error)}
                        helperText={fieldState.error?.message}
                        fullWidth
                      />
                    )}
                  />
                ) : (
                  <Alert severity="info">
                    Không nhập số điện thoại thì chỉ ghi nhận người giám hộ, chưa cấp tài khoản
                    đăng nhập.
                  </Alert>
                )}
              </>
            )}

            <Button
              type="submit"
              variant="contained"
              disabled={save.isPending}
              startIcon={save.isPending ? <CircularProgress size={16} /> : null}
            >
              {editing ? 'Lưu thay đổi' : 'Thêm phụ huynh'}
            </Button>
          </Stack>
        </Box>
      </Drawer>

      <GuardianLinksDrawer
        parent={linksFor}
        token={token}
        onClose={() => setLinksFor(null)}
        onChanged={() => queryClient.invalidateQueries({ queryKey: ['admin', 'parents'] })}
      />
    </Stack>
  );
}

interface GuardianLinksDrawerProps {
  parent: AdminParent | null;
  token: string;
  onClose: () => void;
  onChanged: () => void;
}

function GuardianLinksDrawer({ parent, token, onClose, onChanged }: GuardianLinksDrawerProps) {
  const queryClient = useQueryClient();
  const [studentId, setStudentId] = useState('');
  const [relationship, setRelationship] = useState<Relationship>('FATHER');

  const links = useQuery({
    queryKey: ['admin', 'guardian-links', parent?.id],
    queryFn: () => getGuardianLinks(token, { parentId: parent?.id, inForceOnly: true }),
    enabled: Boolean(parent),
  });

  const students = useQuery({
    queryKey: ['admin', 'students', 'for-linking'],
    queryFn: () => getStudents(token, { page: 0, size: 100, sort: 'fullName,asc' }),
    enabled: Boolean(parent),
  });

  async function refresh() {
    await queryClient.invalidateQueries({ queryKey: ['admin', 'guardian-links', parent?.id] });
    onChanged();
  }

  const link = useMutation({
    mutationFn: () =>
      linkGuardian(token, parent?.id ?? '', {
        studentId,
        relationship,
        contactOrder: (links.data?.length ?? 0) + 1,
      }),
    onSuccess: async () => {
      setStudentId('');
      await refresh();
    },
  });

  const unlink = useMutation({
    mutationFn: (linkId: string) => unlinkGuardian(token, linkId),
    onSuccess: refresh,
  });

  const linkedStudentIds = new Set((links.data ?? []).map((item) => item.studentId));
  const selectable = (students.data?.items ?? []).filter((student) => !linkedStudentIds.has(student.id));

  return (
    <Drawer anchor="right" open={Boolean(parent)} onClose={onClose}>
      <Box sx={{ width: { xs: '100vw', sm: 460 }, p: 3 }}>
        <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
          <Box>
            <Typography variant="h6">Liên kết con</Typography>
            <Typography variant="body2" color="text.secondary">
              {parent?.fullName}
            </Typography>
          </Box>
          <IconButton aria-label="Đóng" onClick={onClose}>
            <CloseRoundedIcon />
          </IconButton>
        </Stack>

        <Stack spacing={2} sx={{ mt: 2 }}>
          {link.isError ? (
            <Alert severity="error">{errorMessage(link.error, 'Không thể liên kết học sinh.')}</Alert>
          ) : null}
          {unlink.isError ? (
            <Alert severity="error">{errorMessage(unlink.error, 'Không thể gỡ liên kết.')}</Alert>
          ) : null}

          {links.isPending ? <Skeleton height={64} /> : null}

          {(links.data ?? []).map((item) => (
            <Card key={item.id} variant="outlined" sx={{ p: 1.5 }}>
              <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography variant="body2" sx={{ fontWeight: 650 }}>
                    {item.studentFullName}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {item.studentCode} · {RELATIONSHIP_LABELS[item.relationship]}
                  </Typography>
                </Box>
                <Tooltip title="Gỡ liên kết">
                  <IconButton
                    aria-label={`Gỡ liên kết với ${item.studentFullName}`}
                    onClick={() => unlink.mutate(item.id)}
                    disabled={unlink.isPending}
                  >
                    <LinkOffRoundedIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              </Stack>
            </Card>
          ))}

          {!links.isPending && (links.data ?? []).length === 0 ? (
            <Typography variant="body2" color="text.secondary">
              Chưa liên kết học sinh nào. Phụ huynh chưa liên kết thì không xem được dữ liệu của ai.
            </Typography>
          ) : null}

          <Divider />

          <FormControl fullWidth>
            <InputLabel id="link-student">Học sinh</InputLabel>
            <Select
              labelId="link-student"
              label="Học sinh"
              value={studentId}
              onChange={(event) => setStudentId(event.target.value)}
            >
              {selectable.map((student) => (
                <MenuItem key={student.id} value={student.id}>
                  {student.fullName} · {student.studentCode}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl fullWidth>
            <InputLabel id="link-relationship">Quan hệ</InputLabel>
            <Select
              labelId="link-relationship"
              label="Quan hệ"
              value={relationship}
              onChange={(event) => setRelationship(event.target.value as Relationship)}
            >
              {(Object.keys(RELATIONSHIP_LABELS) as Relationship[]).map((value) => (
                <MenuItem key={value} value={value}>
                  {RELATIONSHIP_LABELS[value]}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <Button
            variant="contained"
            startIcon={link.isPending ? <CircularProgress size={16} /> : <LinkRoundedIcon />}
            disabled={!studentId || link.isPending}
            onClick={() => link.mutate()}
          >
            Liên kết
          </Button>

          <Alert severity="info">
            Gỡ liên kết chỉ kết thúc hiệu lực, không xoá lịch sử: ai từng xem được dữ liệu của
            học sinh vẫn tra cứu được về sau.
          </Alert>
        </Stack>
      </Box>
    </Drawer>
  );
}
