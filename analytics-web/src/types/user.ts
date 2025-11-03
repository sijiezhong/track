import type { User } from './auth'

export type { User }

export interface UserCreateInput {
  username: string
  password?: string
  realName?: string
  email?: string
  phone?: string
  isAnonymous?: boolean
  appId?: number
}

export interface UserUpdateInput extends Partial<UserCreateInput> {
  id: number
}
