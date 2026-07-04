export function mockGetPatientStats(): Promise<{
  consultCount: number
  appointmentCount: number
  prescriptionCount: number
}> {
  return Promise.resolve({
    consultCount: 3,
    appointmentCount: 2,
    prescriptionCount: 5
  })
}
