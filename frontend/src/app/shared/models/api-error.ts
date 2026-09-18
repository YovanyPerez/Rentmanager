export interface ApiFieldError {
  field: string;
  code: string;
}

export interface ApiError {
  code: string;
  errors?: ApiFieldError[];
}
