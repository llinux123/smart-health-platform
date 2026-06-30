/**
 * 挂号模块 Mock 数据
 */
const mockSchedules = [
  { id: 1, doctorId: 1, doctorName: '王明华', deptName: '皮肤科', workDate: '2026-06-30', shift: 1, shiftName: '上午', totalCount: 30, visibleCount: 12, price: 50.00 },
  { id: 2, doctorId: 2, doctorName: '李秀英', deptName: '内科', workDate: '2026-06-30', shift: 1, shiftName: '上午', totalCount: 40, visibleCount: 5, price: 30.00 },
  { id: 3, doctorId: 3, doctorName: '张建国', deptName: '骨科', workDate: '2026-06-30', shift: 2, shiftName: '下午', totalCount: 25, visibleCount: 0, price: 80.00 },
  { id: 4, doctorId: 1, doctorName: '王明华', deptName: '皮肤科', workDate: '2026-07-01', shift: 1, shiftName: '上午', totalCount: 30, visibleCount: 28, price: 50.00 },
  { id: 5, doctorId: 4, doctorName: '陈晓燕', deptName: '心内科', workDate: '2026-07-01', shift: 2, shiftName: '下午', totalCount: 20, visibleCount: 15, price: 100.00 }
]

// 医生信息映射（ScheduleVO 中只有 doctorId）
export const doctorInfoMap = {
  1: { name: '王明华', title: '主任医师', avatar: '', specialty: '皮肤病、性病、医学美容', intro: '从事皮肤科临床工作20余年，擅长各类皮肤病的诊治。' },
  2: { name: '李秀英', title: '副主任医师', avatar: '', specialty: '呼吸系统疾病、慢性咳嗽', intro: '内科副主任医师，擅长呼吸系统常见病及疑难病的诊治。' },
  3: { name: '张建国', title: '主治医师', avatar: '', specialty: '骨关节疾病、运动损伤', intro: '骨科主治医师，擅长骨折、关节疾病的保守及手术治疗。' },
  4: { name: '陈晓燕', title: '主任医师', avatar: '', specialty: '冠心病、心律失常、高血压', intro: '心内科主任医师，教授，博士生导师，从医30年。' }
}

export function mockGetDoctorDetail(id) {
  const doctor = doctorInfoMap[id]
  if (!doctor) return Promise.reject(new Error('医生不存在'))
  return Promise.resolve({ id, ...doctor, deptName: '' })
}

export function mockListSchedules(params) {
  let result = [...mockSchedules]
  if (params?.deptName) {
    result = result.filter(s => s.deptName === params.deptName)
  }
  if (params?.workDate) {
    result = result.filter(s => s.workDate === params.workDate)
  }
  return Promise.resolve(result)
}

export function mockSeckill(data) {
  return Promise.resolve({
    orderSn: 'REG_20260630_' + String(Math.floor(Math.random() * 9999)).padStart(4, '0'),
    status: 'QUEUING'
  })
}

export function mockGetOrderDetail() {
  return Promise.resolve({
    id: 1,
    orderSn: 'REG_20260630_0001',
    patientId: 100,
    scheduleId: 1,
    doctorId: 1,
    deptName: '皮肤科',
    doctorName: '王明华',
    workDate: '2026-06-30',
    shift: 1,
    shiftName: '上午',
    fee: 50.00,
    status: 2,
    createTime: '2026-06-29 10:30:00',
    payTime: '2026-06-29 10:32:00'
  })
}

export function mockListOrders() {
  return Promise.resolve([
    {
      id: 1,
      orderSn: 'REG_20260630_0001',
      patientId: 100,
      scheduleId: 1,
      doctorId: 1,
      deptName: '皮肤科',
      doctorName: '王明华',
      workDate: '2026-06-30',
      shift: 1,
      shiftName: '上午',
      fee: 50.00,
      status: 2,
      createTime: '2026-06-29 10:30:00',
      payTime: '2026-06-29 10:32:00'
    },
    {
      id: 2,
      orderSn: 'REG_20260628_0003',
      patientId: 100,
      scheduleId: 2,
      doctorId: 2,
      deptName: '内科',
      doctorName: '李秀英',
      workDate: '2026-06-28',
      shift: 1,
      shiftName: '上午',
      fee: 30.00,
      status: 3,
      createTime: '2026-06-27 15:20:00',
      payTime: '2026-06-27 15:21:00'
    },
    {
      id: 3,
      orderSn: 'REG_20260625_0010',
      patientId: 100,
      scheduleId: 4,
      doctorId: 1,
      deptName: '皮肤科',
      doctorName: '王明华',
      workDate: '2026-06-25',
      shift: 1,
      shiftName: '上午',
      fee: 50.00,
      status: 1,
      createTime: '2026-06-29 08:00:00'
    }
  ])
}

export function mockCancelOrder(orderSn) {
  return Promise.resolve()
}

export function mockPayOrder(orderSn) {
  return Promise.resolve()
}
