import { onMounted, ref, type Ref } from "vue";
import { request } from "../services/http";
export interface Page<T> {
  content: T[];
  totalElements: number;
  number: number;
  size: number;
}
export function usePagedList<T>(
  endpoint: string,
  filters: () => Record<
    string,
    string | number | null | undefined
  > = () => ({}),
  initialSize = 50,
) {
  const rows = ref<T[]>([]) as Ref<T[]>;
  const keyword = ref("");
  const page = ref(0);
  const size = ref(initialSize);
  const total = ref(0);
  const loading = ref(false);
  const error = ref("");
  let sequence = 0;
  async function load() {
    const current = ++sequence;
    loading.value = true;
    error.value = "";
    const params = new URLSearchParams({
      keyword: keyword.value.trim(),
      page: String(page.value),
      size: String(size.value),
    });
    Object.entries(filters()).forEach(([key, value]) => {
      if (value != null && value !== "") params.set(key, String(value));
    });
    try {
      const result = await request<Page<T>>(`${endpoint}?${params}`);
      if (current === sequence) {
        rows.value = result.content;
        total.value = result.totalElements;
      }
    } catch (failure) {
      if (current === sequence)
        error.value =
          failure instanceof Error
            ? failure.message
            : "No se pudieron cargar los registros.";
    } finally {
      if (current === sequence) loading.value = false;
    }
  }
  function search() {
    page.value = 0;
    return load();
  }
  function paginate(next: number, count: number) {
    page.value = next;
    size.value = count;
    return load();
  }
  onMounted(load);
  return {
    rows,
    keyword,
    page,
    size,
    total,
    loading,
    error,
    load,
    search,
    paginate,
  };
}
