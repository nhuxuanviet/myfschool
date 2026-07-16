import { createTheme } from '@mui/material/styles';

export const adminTheme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#f4511e',
      dark: '#c6280d',
      light: '#ff8a65',
      contrastText: '#ffffff',
    },
    background: {
      default: '#f7f8fa',
      paper: '#ffffff',
    },
    text: {
      primary: '#17202e',
      secondary: '#687386',
    },
    divider: '#e4e7ec',
  },
  shape: { borderRadius: 10 },
  typography: {
    fontFamily: '"Inter Variable", Inter, "Segoe UI", sans-serif',
    h1: { fontSize: '1.5rem', lineHeight: 1.3, fontWeight: 720, letterSpacing: '-0.02em' },
    h2: { fontSize: '1.05rem', lineHeight: 1.4, fontWeight: 680 },
    body1: { fontSize: '0.925rem', lineHeight: 1.55 },
    body2: { fontSize: '0.825rem', lineHeight: 1.5 },
    button: { fontWeight: 650, textTransform: 'none' },
  },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: { root: { minHeight: 40, borderRadius: 9 } },
    },
    MuiTextField: { defaultProps: { size: 'small' } },
    MuiCard: {
      styleOverrides: {
        root: { border: '1px solid #e5e8ee', boxShadow: '0 1px 2px rgba(16, 24, 40, 0.03)' },
      },
    },
  },
});
