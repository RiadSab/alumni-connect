// Job board filter control values, kept out of the component file so fast refresh
// stays happy. "ALL" is the sentinel for "no filter" (Radix Select can't use an
// empty-string value); the page turns these into JobOfferFilters.

export interface JobFilterValues {
  q: string;
  city: string;
  employmentType: string;
  isRemote: boolean;
  skills: string[];
}

export const EMPTY_FILTERS: JobFilterValues = {
  q: "",
  city: "ALL",
  employmentType: "ALL",
  isRemote: false,
  skills: [],
};
