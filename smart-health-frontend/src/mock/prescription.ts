interface PrescriptionItem {
  id: number
  medicineName: string
  spec: string
  usage: string
  quantity: number
  unit: string
  price: number
}

interface Prescription {
  id: number
  prescriptionSn: string
  patientId: number
  doctorId: number
  diagnosis: string
  pdfUrl?: string
  auditStatus: number
  pharmacistId?: number
  auditComments?: string
  auditTime?: string
  status: number
  createTime: string
  items: PrescriptionItem[]
}

export function mockListPrescriptions(): Promise<Prescription[]> {
  return Promise.resolve([
    {
      id: 1,
      prescriptionSn: 'RX_H001_20260628_0001',
      patientId: 100,
      doctorId: 1,
      diagnosis: '湿疹，建议外用糖皮质激素软膏',
      pdfUrl: '/prescription-pdfs/RX_H001_20260628_0001.pdf',
      auditStatus: 1,
      pharmacistId: 200,
      auditComments: '用药合理，同意',
      auditTime: '2026-06-28 16:00:00',
      status: 2,
      createTime: '2026-06-28 14:30:00',
      items: [
        { id: 1, medicineName: '卤米松乳膏', spec: '15g/支', usage: '外用，每日2次', quantity: 2, unit: '支', price: 25.50 },
        { id: 2, medicineName: '氯雷他定片', spec: '10mg*12片', usage: '口服，每日1次', quantity: 1, unit: '盒', price: 18.00 }
      ]
    },
    {
      id: 2,
      prescriptionSn: 'RX_H001_20260625_0003',
      patientId: 100,
      doctorId: 2,
      diagnosis: '急性上呼吸道感染',
      pdfUrl: '/prescription-pdfs/RX_H001_20260625_0003.pdf',
      auditStatus: 0,
      status: 0,
      createTime: '2026-06-25 10:00:00',
      items: [
        { id: 3, medicineName: '阿莫西林胶囊', spec: '0.5g*24粒', usage: '口服，每日3次', quantity: 1, unit: '盒', price: 12.50 },
        { id: 4, medicineName: '布洛芬缓释胶囊', spec: '0.3g*20粒', usage: '口服，发热时服用', quantity: 1, unit: '盒', price: 15.00 },
        { id: 5, medicineName: '复方甘草片', spec: '100片/瓶', usage: '口服，每日3次', quantity: 1, unit: '瓶', price: 8.00 }
      ]
    },
    {
      id: 3,
      prescriptionSn: 'RX_H001_20260620_0005',
      patientId: 100,
      doctorId: 4,
      diagnosis: '高血压2级，高血脂',
      pdfUrl: '/prescription-pdfs/RX_H001_20260620_0005.pdf',
      auditStatus: 2,
      auditComments: '药品用量偏大，请调整',
      auditTime: '2026-06-20 15:30:00',
      status: 0,
      createTime: '2026-06-20 09:00:00',
      items: [
        { id: 6, medicineName: '氨氯地平片', spec: '5mg*28片', usage: '口服，每日1次', quantity: 1, unit: '盒', price: 35.00 },
        { id: 7, medicineName: '阿托伐他汀钙片', spec: '20mg*7片', usage: '口服，每晚1次', quantity: 2, unit: '盒', price: 42.00 }
      ]
    }
  ])
}

export function mockGetPrescription(): Promise<Prescription> {
  return Promise.resolve({
    id: 1,
    prescriptionSn: 'RX_H001_20260628_0001',
    patientId: 100,
    doctorId: 1,
    diagnosis: '湿疹，建议外用糖皮质激素软膏',
    pdfUrl: '/prescription-pdfs/RX_H001_20260628_0001.pdf',
    auditStatus: 1,
    pharmacistId: 200,
    auditComments: '用药合理，同意',
    auditTime: '2026-06-28 16:00:00',
    status: 2,
    createTime: '2026-06-28 14:30:00',
    items: [
      { id: 1, medicineName: '卤米松乳膏', spec: '15g/支', usage: '外用，每日2次', quantity: 2, unit: '支', price: 25.50 },
      { id: 2, medicineName: '氯雷他定片', spec: '10mg*12片', usage: '口服，每日1次', quantity: 1, unit: '盒', price: 18.00 }
    ]
  })
}

export function mockListPendingAudit(): Promise<Prescription[]> {
  return Promise.resolve([
    {
      id: 2,
      prescriptionSn: 'RX_H001_20260625_0003',
      patientId: 100,
      doctorId: 2,
      diagnosis: '急性上呼吸道感染',
      auditStatus: 0,
      status: 0,
      createTime: '2026-06-25 10:00:00',
      items: [
        { id: 3, medicineName: '阿莫西林胶囊', spec: '0.5g*24粒', usage: '口服，每日3次', quantity: 1, unit: '盒', price: 12.50 },
        { id: 4, medicineName: '布洛芬缓释胶囊', spec: '0.3g*20粒', usage: '口服，发热时服用', quantity: 1, unit: '盒', price: 15.00 }
      ]
    }
  ])
}
