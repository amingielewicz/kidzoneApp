import {
  Box,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Tooltip,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import CancelIcon from '@mui/icons-material/Cancel';
import VisibilityIcon from '@mui/icons-material/Visibility';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import TableSortLabel from '@mui/material/TableSortLabel';
import { ReviewReport, REVIEW_REPORT_REASON_LABELS, ReviewReportReason } from '../../types';
import { statusChip, formatDate, copyToClipboard } from './reportUtils';

interface ReviewReportsTableProps {
  reports: ReviewReport[];
  sortDir: 'asc' | 'desc';
  onToggleSort: () => void;
  onViewDetail: (report: ReviewReport) => void;
  onDelete: (report: ReviewReport) => void;
  onDismiss: (report: ReviewReport) => void;
}

export function ReviewReportsTable({
  reports,
  sortDir,
  onToggleSort,
  onViewDetail,
  onDelete,
  onDismiss,
}: ReviewReportsTableProps) {
  return (
    <TableContainer component={Paper}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell sx={{ width: 140 }}>
              <TableSortLabel active direction={sortDir} onClick={onToggleSort}>
                Data
              </TableSortLabel>
            </TableCell>
            <TableCell>Review ID</TableCell>
            <TableCell sx={{ width: 180 }}>Powód</TableCell>
            <TableCell>Komentarz</TableCell>
            <TableCell sx={{ width: 110 }}>Status</TableCell>
            <TableCell sx={{ width: 100 }}>Akcje</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {reports.map((report) => (
            <TableRow key={report.id} hover>
              <TableCell>{formatDate(report.createdAtMillis)}</TableCell>
              <TableCell>
                <Box display="flex" alignItems="center" gap={0.5}>
                  <Tooltip title={report.reviewId}>
                    <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: 11 }}>
                      {report.reviewId.slice(0, 12)}...
                    </Typography>
                  </Tooltip>
                  <Tooltip title="Kopiuj ID">
                    <IconButton size="small" onClick={() => copyToClipboard(report.reviewId)}>
                      <ContentCopyIcon sx={{ fontSize: 14 }} />
                    </IconButton>
                  </Tooltip>
                </Box>
              </TableCell>
              <TableCell>
                {REVIEW_REPORT_REASON_LABELS[report.reason as ReviewReportReason] || report.reason}
              </TableCell>
              <TableCell>
                <Tooltip title={report.comment || ''}>
                  <Typography variant="body2" noWrap>
                    {report.comment || '\u2014'}
                  </Typography>
                </Tooltip>
              </TableCell>
              <TableCell>{statusChip(report.status)}</TableCell>
              <TableCell>
                <Box display="flex" flexDirection="row" alignItems="flex-start">
                  <Tooltip title="Szczegóły">
                    <IconButton size="small" onClick={() => onViewDetail(report)}>
                      <VisibilityIcon />
                    </IconButton>
                  </Tooltip>
                  {report.status === 'pending' && (
                    <Box display="flex" flexDirection="column">
                      <Tooltip title="Usun opinię">
                        <IconButton color="error" size="small" onClick={() => onDelete(report)}>
                          <DeleteIcon />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Odrzuc">
                        <IconButton
                          size="small"
                          sx={{ color: '#1976D2' }}
                          onClick={() => onDismiss(report)}
                        >
                          <CancelIcon />
                        </IconButton>
                      </Tooltip>
                    </Box>
                  )}
                </Box>
              </TableCell>
            </TableRow>
          ))}
          {reports.length === 0 && (
            <TableRow>
              <TableCell colSpan={6} align="center">
                Brak zgloszen
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
