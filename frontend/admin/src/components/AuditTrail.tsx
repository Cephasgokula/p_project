import React, { useMemo, useCallback } from "react";
import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { AgGridReact } from "ag-grid-react";
import "ag-grid-community/styles/ag-grid.css";
import "ag-grid-community/styles/ag-theme-alpine.css";
import type { ColDef, ValueFormatterParams } from "ag-grid-community";

const API = process.env.REACT_APP_API_URL || "http://localhost:8080/api/v1";

interface DecisionRow {
  id: string;
  applicationId: string;
  applicantName: string;
  decision: "APPROVE" | "REFER" | "DECLINE";
  finalScore: number;
  dtScore: number;
  mlScore: number;
  fairnessScore: number;
  fraudProbability: number;
  createdAt: string;
}

export default function AuditTrail() {
  const { data: rows = [] } = useQuery<DecisionRow[]>({
    queryKey: ["audit-decisions"],
    queryFn: () => axios.get(`${API}/admin/decisions`).then((r) => r.data),
  });

  const columns = useMemo<ColDef<DecisionRow>[]>(() => [
    { field: "applicationId", headerName: "App ID", width: 130,
      valueFormatter: (p: ValueFormatterParams) => (p.value as string)?.slice(0, 8) + "…" },
    { field: "applicantName", headerName: "Name", flex: 1 },
    {
      field: "decision", headerName: "Decision", width: 110,
      cellStyle: (p) => ({
        color: p.value === "APPROVE" ? "#16A34A" : p.value === "DECLINE" ? "#DC2626" : "#D97706",
        fontWeight: 600,
      }),
    },
    { field: "finalScore", headerName: "Final", width: 80, type: "numericColumn" },
    { field: "dtScore", headerName: "DT", width: 70, type: "numericColumn" },
    { field: "mlScore", headerName: "ML", width: 70, type: "numericColumn" },
    { field: "fairnessScore", headerName: "Fair", width: 70, type: "numericColumn" },
    { field: "fraudProbability", headerName: "Fraud%", width: 90, type: "numericColumn",
      valueFormatter: (p: ValueFormatterParams) => `${((p.value as number) * 100).toFixed(1)}%` },
    { field: "createdAt", headerName: "Date", width: 160,
      valueFormatter: (p: ValueFormatterParams) => new Date(p.value as string).toLocaleString() },
  ], []);

  const defaultColDef = useMemo(() => ({
    sortable: true,
    filter: true,
    resizable: true,
  }), []);

  const onGridReady = useCallback(() => {}, []);

  return (
    <div>
      <h2 className="text-2xl font-bold text-slate-800 mb-6">Audit Trail</h2>
      <div className="ag-theme-alpine rounded-xl overflow-hidden border border-slate-200" style={{ height: 600 }}>
        <AgGridReact<DecisionRow>
          rowData={rows}
          columnDefs={columns}
          defaultColDef={defaultColDef}
          onGridReady={onGridReady}
          pagination
          paginationPageSize={25}
          animateRows
        />
      </div>
    </div>
  );
}
