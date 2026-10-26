import { defineComponent } from "vue";
import { flushPromises, mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import { request } from "../services/http";
import { usePagedList } from "./usePagedList";

vi.mock("../services/http", () => ({ request: vi.fn() }));

describe("Listados paginados", () => {
  it("inicia con 50 registros y conserva los filtros al cambiar tamaño y buscar", async () => {
    vi.mocked(request).mockResolvedValue({ content: [], totalElements: 0 });
    let list!: ReturnType<typeof usePagedList>;
    const wrapper = mount(
      defineComponent({
        setup() {
          list = usePagedList("/web/api/licenses", () => ({
            clientId: 7,
            status: "ACTIVE",
          }));
          return () => null;
        },
      }),
    );
    await flushPromises();
    expect(request).toHaveBeenLastCalledWith(
      "/web/api/licenses?keyword=&page=0&size=50&clientId=7&status=ACTIVE",
    );
    await list.paginate(2, 25);
    list.keyword.value = " Empresa ";
    await list.search();
    expect(request).toHaveBeenLastCalledWith(
      "/web/api/licenses?keyword=Empresa&page=0&size=25&clientId=7&status=ACTIVE",
    );
    await list.paginate(0, 100);
    expect(request).toHaveBeenLastCalledWith(
      "/web/api/licenses?keyword=Empresa&page=0&size=100&clientId=7&status=ACTIVE",
    );
    wrapper.unmount();
  });
});
